package org.example.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.dao.CVDao;
import org.example.dao.UserDao;
import org.example.dto.CVDto;
import org.example.model.CV;
import org.example.model.Candidate;
import org.example.model.Role;
import org.example.security.Secured;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import java.time.Duration;

@Path("/cv")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CVController {

    @Inject
    private CVDao cvDao;

    @Inject
    private UserDao userDao;

    private static final String UPLOAD_DIR = System.getProperty("java.io.tmpdir") + "/cv_uploads/";

    @POST
    @Secured({Role.CANDIDATE})
    public Response uploadCV(CVDto dto, @Context ContainerRequestContext requestContext) {
        Long candidateId = (Long) requestContext.getProperty("userId");
        Candidate candidate = (Candidate) userDao.findById(candidateId);

        if (candidate == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            File dir = new File(UPLOAD_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String safeFileName = UUID.randomUUID().toString() + "_" + dto.getFileName();
            String filePath = UPLOAD_DIR + safeFileName;

            byte[] decodedBytes = Base64.getDecoder().decode(dto.getBase64Content());
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(decodedBytes);
            }

            // Delete old CV if exists
            CV existingCv = cvDao.findByCandidateId(candidateId);
            if (existingCv != null) {
                File oldFile = new File(existingCv.getFilePath());
                if (oldFile.exists()) {
                    oldFile.delete();
                }
                cvDao.delete(existingCv.getId());
            }

            CV cv = new CV();
            cv.setFileName(dto.getFileName());
            cv.setFilePath(filePath);
            cv.setCandidate(candidate);

            // Call Python AI Service for general improvements
            try {
                String payload = String.format("{\"file_path\":\"%s\"}", filePath.replace("\\", "/"));
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8000/analyze_cv_general"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(120))
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Quick and dirty JSON parsing for {"improvements": "..."}
                    String body = response.body();
                    int impIndex = body.indexOf("\"improvements\":");
                    if (impIndex != -1) {
                        int startQuote = body.indexOf("\"", impIndex + 15);
                        int endQuote = body.lastIndexOf("\"");
                        if (startQuote != -1 && endQuote > startQuote) {
                            String improvements = body.substring(startQuote + 1, endQuote)
                                    .replace("\\n", "\n")
                                    .replace("\\\"", "\"");
                            cv.setAiImprovements(improvements);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            cvDao.save(cv);

            return Response.status(Response.Status.CREATED).entity(cv).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("{\"error\":\"Failed to upload CV: " + e.getMessage() + "\"}").build();
        }
    }

    @GET
    @Secured({Role.CANDIDATE, Role.RECRUITER})
    @Path("/candidate/{candidateId}")
    public Response getCV(@PathParam("candidateId") Long candidateId) {
        CV cv = cvDao.findByCandidateId(candidateId);
        if (cv == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(cv).build();
    }

    @GET
    @Secured({Role.CANDIDATE})
    @Path("/me")
    public Response getMyCV(@Context ContainerRequestContext requestContext) {
        Long candidateId = (Long) requestContext.getProperty("userId");
        CV cv = cvDao.findByCandidateId(candidateId);
        if (cv == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(cv).build();
    }

    @GET
    @Secured({Role.RECRUITER, Role.CANDIDATE})
    @Path("/download/{candidateId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadCV(@PathParam("candidateId") Long candidateId) {
        CV cv = cvDao.findByCandidateId(candidateId);
        if (cv == null || cv.getFilePath() == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        File file = new File(cv.getFilePath());
        if (!file.exists()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Response.ResponseBuilder response = Response.ok((Object) file);
        response.header("Content-Disposition", "attachment; filename=\"" + cv.getFileName() + "\"");
        return response.build();
    }
}

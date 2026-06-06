package org.example.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.dao.ApplicationDao;
import org.example.dao.JobOfferDao;
import org.example.dao.UserDao;
import org.example.dto.ApplicationDto;
import org.example.model.Application;
import org.example.model.Candidate;
import org.example.model.JobOffer;
import org.example.model.Role;
import org.example.model.CV;
import org.example.dao.CVDao;
import org.example.security.Secured;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Path("/applications")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ApplicationController {

    @Inject
    private ApplicationDao applicationDao;

    @Inject
    private JobOfferDao jobOfferDao;

    @Inject
    private UserDao userDao;

    @Inject
    private CVDao cvDao;

    @POST
    @Secured({Role.CANDIDATE})
    public Response apply(ApplicationDto dto, @Context ContainerRequestContext requestContext) {
        Long candidateId = (Long) requestContext.getProperty("userId");
        Candidate candidate = (Candidate) userDao.findById(candidateId);
        JobOffer offer = jobOfferDao.findById(dto.getJobOfferId());

        if (candidate == null || offer == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Invalid candidate or offer\"}").build();
        }

        // Check if already applied
        List<Application> existing = applicationDao.findByCandidateId(candidateId);
        for (Application a : existing) {
            if (a.getJobOffer().getId().equals(offer.getId())) {
                return Response.status(Response.Status.CONFLICT).entity("{\"error\":\"Already applied\"}").build();
            }
        }

        Application application = new Application();
        application.setCandidate(candidate);
        application.setJobOffer(offer);

        // Fetch Candidate's CV
        CV cv = cvDao.findByCandidateId(candidateId);
        if (cv != null) {
            try {
                // Prepare JSON payload for Python Microservice
                String skillsJson = "[]";
                if (offer.getRequiredSkills() != null && !offer.getRequiredSkills().isEmpty()) {
                    skillsJson = "[\"" + String.join("\",\"", offer.getRequiredSkills()) + "\"]";
                }
                
                String payload = String.format("{\"file_path\":\"%s\", \"required_skills\":%s}", 
                        cv.getFilePath().replace("\\", "/"), skillsJson);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8000/analyze"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofSeconds(120))
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpClient client = HttpClient.newHttpClient();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String body = response.body();
                    // Parse "score"
                    int scoreIndex = body.indexOf("\"score\":");
                    if (scoreIndex != -1) {
                        int commaIndex = body.indexOf(",", scoreIndex);
                        int braceIndex = body.indexOf("}", scoreIndex);
                        int endIdx = (commaIndex != -1 && commaIndex < braceIndex) ? commaIndex : braceIndex;
                        String scoreStr = body.substring(scoreIndex + 8, endIdx).trim();
                        application.setScore(Integer.parseInt(scoreStr));
                    }
                    // Parse "strongest_points"
                    int pointsIndex = body.indexOf("\"strongest_points\":");
                    if (pointsIndex != -1) {
                        int startQuote = body.indexOf("\"", pointsIndex + 19);
                        int endQuote = body.lastIndexOf("\"");
                        if (startQuote != -1 && endQuote > startQuote) {
                            String strongestPoints = body.substring(startQuote + 1, endQuote)
                                    .replace("\\n", "\n")
                                    .replace("\\\"", "\"");
                            application.setAiStrongestPoints(strongestPoints);
                        }
                    }
                }
            } catch (Exception e) {
                // Log error but allow application to proceed without score
                e.printStackTrace();
            }
        }

        applicationDao.save(application);

        return Response.status(Response.Status.CREATED).entity(application).build();
    }

    @GET
    @Path("/job/{jobOfferId}")
    @Secured({Role.RECRUITER})
    public Response getApplicationsForJob(@PathParam("jobOfferId") Long jobOfferId, @Context ContainerRequestContext requestContext) {
        Long recruiterId = (Long) requestContext.getProperty("userId");
        JobOffer offer = jobOfferDao.findById(jobOfferId);

        if (offer == null || !offer.getRecruiter().getId().equals(recruiterId)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        List<Application> applications = applicationDao.findByJobOfferId(jobOfferId);
        return Response.ok(applications).build();
    }

    @GET
    @Path("/me")
    @Secured({Role.CANDIDATE})
    public Response getMyApplications(@Context ContainerRequestContext requestContext) {
        Long candidateId = (Long) requestContext.getProperty("userId");
        List<Application> applications = applicationDao.findByCandidateId(candidateId);
        return Response.ok(applications).build();
    }

    @PUT
    @Path("/{id}/status")
    @Secured({Role.RECRUITER})
    public Response updateStatus(@PathParam("id") Long id, java.util.Map<String, String> payload, @Context ContainerRequestContext requestContext) {
        Long recruiterId = (Long) requestContext.getProperty("userId");
        Application app = applicationDao.findById(id);
        if (app == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (!app.getJobOffer().getRecruiter().getId().equals(recruiterId)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        
        String statusStr = payload.get("status");
        if (statusStr != null) {
            try {
                org.example.model.ApplicationStatus newStatus = org.example.model.ApplicationStatus.valueOf(statusStr.toUpperCase());
                app.setStatus(newStatus);
                applicationDao.update(app);
                return Response.ok(app).build();
            } catch (IllegalArgumentException e) {
                return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Invalid status\"}").build();
            }
        }
        return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Missing status\"}").build();
    }
}

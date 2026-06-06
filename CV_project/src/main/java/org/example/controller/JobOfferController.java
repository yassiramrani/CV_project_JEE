package org.example.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.dao.JobOfferDao;
import org.example.dao.UserDao;
import org.example.dto.JobOfferDto;
import org.example.model.JobOffer;
import org.example.model.Recruiter;
import org.example.model.Role;
import org.example.security.Secured;

import java.util.List;

@Path("/jobs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class JobOfferController {

    @Inject
    private JobOfferDao jobOfferDao;

    @Inject
    private UserDao userDao;

    @GET
    public Response getAllJobs() {
        List<JobOffer> jobs = jobOfferDao.findAll();
        return Response.ok(jobs).build();
    }

    @GET
    @Path("/{id}")
    public Response getJob(@PathParam("id") Long id) {
        JobOffer job = jobOfferDao.findById(id);
        if (job == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(job).build();
    }

    @POST
    @Secured({Role.RECRUITER})
    public Response createJob(JobOfferDto dto, @Context ContainerRequestContext requestContext) {
        Long recruiterId = (Long) requestContext.getProperty("userId");
        Recruiter recruiter = (Recruiter) userDao.findById(recruiterId);

        if (recruiter == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        JobOffer offer = new JobOffer();
        offer.setTitle(dto.getTitle());
        offer.setDescription(dto.getDescription());
        offer.setLocation(dto.getLocation());
        offer.setRequiredSkills(dto.getRequiredSkills());
        offer.setRecruiter(recruiter);

        jobOfferDao.save(offer);

        return Response.status(Response.Status.CREATED).entity(offer).build();
    }

    @DELETE
    @Path("/{id}")
    @Secured({Role.RECRUITER})
    public Response deleteJob(@PathParam("id") Long id, @Context ContainerRequestContext requestContext) {
        Long recruiterId = (Long) requestContext.getProperty("userId");
        JobOffer job = jobOfferDao.findById(id);

        if (job == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!job.getRecruiter().getId().equals(recruiterId)) {
            return Response.status(Response.Status.FORBIDDEN).entity("{\"error\":\"Not your job offer\"}").build();
        }

        jobOfferDao.delete(id);
        return Response.noContent().build();
    }
}

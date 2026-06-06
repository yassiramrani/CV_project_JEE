package org.example.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.example.dao.UserDao;
import org.example.dto.AuthResponseDto;
import org.example.dto.LoginDto;
import org.example.dto.RegisterDto;
import org.example.model.Candidate;
import org.example.model.Recruiter;
import org.example.model.Role;
import org.example.model.User;
import org.example.security.JwtUtil;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    @Inject
    private UserDao userDao;

    @POST
    @Path("/register")
    public Response register(RegisterDto dto) {
        if (userDao.findByEmail(dto.getEmail()) != null) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"Email already in use\"}")
                    .build();
        }

        User user;
        Role role = Role.valueOf(dto.getRole().toUpperCase());

        if (role == Role.RECRUITER) {
            Recruiter r = new Recruiter();
            r.setCompanyName(dto.getCompanyName());
            user = r;
        } else if (role == Role.CANDIDATE) {
            Candidate c = new Candidate();
            c.setPhone(dto.getPhone());
            c.setAddress(dto.getAddress());
            user = c;
        } else {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Invalid role\"}")
                    .build();
        }

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword()); // In production, hash the password!
        user.setRole(role);

        userDao.save(user);

        return Response.status(Response.Status.CREATED)
                .entity("{\"message\":\"User registered successfully\"}")
                .build();
    }

    @POST
    @Path("/login")
    public Response login(LoginDto dto) {
        User user = userDao.findByEmail(dto.getEmail());
        
        if (user == null || !user.getPassword().equals(dto.getPassword())) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Invalid email or password\"}")
                    .build();
        }

        String token = JwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole());
        AuthResponseDto responseDto = new AuthResponseDto(token, user.getRole(), user.getEmail(), user.getFirstName(), user.getLastName());

        return Response.ok(responseDto).build();
    }
}

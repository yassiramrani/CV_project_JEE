package org.example.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        System.out.println(">>> CorsFilter REQUEST: " + requestContext.getMethod() + " for " + requestContext.getUriInfo().getPath());
        if (requestContext.getMethod().equalsIgnoreCase("OPTIONS")) {
            Response.ResponseBuilder builder = Response.ok();
            String origin = requestContext.getHeaderString("Origin");
            builder.header("Access-Control-Allow-Origin", origin != null ? origin : "*");
            builder.header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
            builder.header("Access-Control-Allow-Credentials", "true");
            builder.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
            requestContext.abortWith(builder.build());
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext, 
                       ContainerResponseContext responseContext) throws IOException {
        System.out.println(">>> CorsFilter RESPONSE: " + requestContext.getMethod() + " for " + requestContext.getUriInfo().getPath());
        String origin = requestContext.getHeaderString("Origin");
        responseContext.getHeaders().putSingle("Access-Control-Allow-Origin", origin != null ? origin : "*");
        responseContext.getHeaders().putSingle("Access-Control-Allow-Headers", "origin, content-type, accept, authorization");
        responseContext.getHeaders().putSingle("Access-Control-Allow-Credentials", "true");
        responseContext.getHeaders().putSingle("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }
}

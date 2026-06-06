package org.example.security;

import io.jsonwebtoken.Claims;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.example.model.Role;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;

@Provider
@Secured
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            abortWithUnauthorized(requestContext);
            return;
        }

        String token = authorizationHeader.substring("Bearer".length()).trim();

        try {
            Claims claims = JwtUtil.validateToken(token);
            String roleStr = claims.get("role", String.class);
            Role userRole = Role.valueOf(roleStr);

            // Check if route has specific role requirements
            Method method = resourceInfo.getResourceMethod();
            Secured secured = method.getAnnotation(Secured.class);
            if (secured == null) {
                secured = resourceInfo.getResourceClass().getAnnotation(Secured.class);
            }
            
            if (secured != null && secured.value().length > 0) {
                boolean isAllowed = Arrays.asList(secured.value()).contains(userRole);
                if (!isAllowed) {
                    requestContext.abortWith(
                        Response.status(Response.Status.FORBIDDEN)
                                .entity("{\"error\":\"Forbidden: Insufficient privileges\"}")
                                .build());
                }
            }

            // Set user info in context for controllers to access
            requestContext.setProperty("userId", claims.get("userId", Long.class));
            requestContext.setProperty("userEmail", claims.getSubject());
            requestContext.setProperty("userRole", userRole);

        } catch (Exception e) {
            abortWithUnauthorized(requestContext);
        }
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext) {
        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .entity("{\"error\":\"Unauthorized: Invalid or missing token\"}")
                        .build());
    }
}

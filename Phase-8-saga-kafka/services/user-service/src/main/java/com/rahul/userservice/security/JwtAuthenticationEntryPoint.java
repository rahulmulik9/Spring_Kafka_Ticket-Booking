package com.rahul.userservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Called by Spring Security when an unauthenticated request hits a protected URL.
     * Writes a 401 response using the same fields as our ErrorResponse class.
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {

        // Built by hand because we are outside Spring MVC here, so ResponseEntity and
        // ErrorResponse cannot be returned. The fields match ErrorResponse exactly.
        String body = String.format(
                "{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"%s\",\"path\":\"%s\",\"timestamp\":\"%s\"}",
                "Authentication required. Please log in.",
                request.getRequestURI(),
                LocalDateTime.now());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(body);
    }
}
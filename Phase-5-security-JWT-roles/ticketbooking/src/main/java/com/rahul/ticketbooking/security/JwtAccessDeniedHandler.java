package com.rahul.ticketbooking.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Formats the response when a request is rejected because the caller IS authenticated
 * but does not have the required role (403).
 *
 * Partner of JwtAuthenticationEntryPoint, which handles "not authenticated" (401).
 * Like the entry point, this runs inside the filter chain, so GlobalExceptionHandler cannot catch it.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        String body = String.format(
                "{\"status\":403,\"error\":\"FORBIDDEN\",\"message\":\"%s\",\"path\":\"%s\",\"timestamp\":\"%s\"}",
                "You do not have permission to perform this action.",
                request.getRequestURI(),
                LocalDateTime.now());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write(body);
    }
}
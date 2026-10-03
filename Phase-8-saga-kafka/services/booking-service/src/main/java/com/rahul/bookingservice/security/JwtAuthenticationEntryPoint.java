package com.rahul.bookingservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Formats the response when a request is rejected because the caller is NOT authenticated
 * (no token, expired token, or tampered token).
 *
 * It does not decide who gets blocked. The JWT filter and the URL rules in SecurityConfig do that.
 * This class only decides what the client sees: a 401 with our standard error JSON.
 *
 * Why it is needed: security rejections happen inside the filter chain, before any controller runs,
 * so GlobalExceptionHandler (@RestControllerAdvice) never sees them. Without this class,
 * Spring would send its default empty 401 response.
 *
 * Its partner for "logged in but not allowed" (403) is the access denied handler, added in Step 5.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * Called by Spring Security when an unauthenticated request hits a protected URL.
     * Writes a 401 response using the same fields as our ErrorResponse class.
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

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
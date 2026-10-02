package com.rahul.apigateway.filter;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;

// Runs on every request that passes through the Gateway, before it is forwarded.
// It only checks that the token is genuine. The services still read the token themselves.
@Component
public class JwtGatewayFilter implements GlobalFilter, Ordered {

    // Calls that need no token. They mirror the public rules inside the services.
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/auth/logout",
            "/actuator/health");

    private final SecretKey key;

    public JwtGatewayFilter(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isPublic(request)) {
            return chain.filter(exchange);
        }

        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return reject(exchange, "Missing or malformed Authorization header.");
        }

        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(header.substring(7));
        } catch (JwtException | IllegalArgumentException ex) {
            return reject(exchange, "Invalid or expired token.");
        }

        // The token goes through unchanged, so the service behind can check it too.
        return chain.filter(exchange);
    }

    private boolean isPublic(ServerHttpRequest request) {
        String path = request.getPath().value();

        if (PUBLIC_PATHS.contains(path)) {
            return true;
        }
        // Browsing movies, shows and seats is open, but only for reading
        boolean isRead = HttpMethod.GET.equals(request.getMethod());
        return isRead && (path.startsWith("/movies") || path.matches("/shows/[^/]+/seats"));
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = "{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"" + message
                + "\",\"path\":\"" + exchange.getRequest().getPath().value()
                + "\",\"timestamp\":\"" + LocalDateTime.now() + "\"}";

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    // Run before the filters that forward the request
    @Override
    public int getOrder() {
        return -1;
    }
}
package com.rahul.ticketbooking.config;

import com.rahul.ticketbooking.security.JwtAccessDeniedHandler;
import com.rahul.ticketbooking.security.JwtAuthenticationEntryPoint;
import com.rahul.ticketbooking.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)   // 401: not logged in
                        .accessDeniedHandler(jwtAccessDeniedHandler))            // 403: logged in, wrong role
                .authorizeHttpRequests(auth -> auth
                        // 1. Public: no token needed
                        .requestMatchers("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                                "/actuator/health", "/public").permitAll() // 2. Public browsing: only GET (read) requests
                        .requestMatchers(HttpMethod.GET, "/movies/**", "/shows/*/seats").permitAll()
                        // 3. Creating movies and shows: organizers and admins only
                        .requestMatchers(HttpMethod.POST, "/movies", "/movies/*/shows")
                        .hasAnyRole("ORGANIZER", "ADMIN")
                        // 4. Other actuator endpoints: admin only
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // 5. Everything else (bookings, /api/auth/me, ...): any logged-in user
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
package com.rahul.cinemaservice.config;

import com.rahul.cinemaservice.security.JwtAccessDeniedHandler;
import com.rahul.cinemaservice.security.JwtAuthenticationEntryPoint;
import com.rahul.cinemaservice.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)   // 401: not logged in
                        .accessDeniedHandler(jwtAccessDeniedHandler))            // 403: logged in, wrong role
                .authorizeHttpRequests(auth -> auth
                        // 1. Public: no token needed
                        .requestMatchers("/actuator/health").permitAll()
                        // 2. Public browsing: only GET (read) requests
                        .requestMatchers(HttpMethod.GET, "/movies/**", "/shows/*/seats").permitAll()
                        // 3. Creating movies and shows: organizers and admins only
                        .requestMatchers(HttpMethod.POST, "/movies", "/movies/*/shows")
                        .hasAnyRole("ORGANIZER", "ADMIN")
                        // 4. Other actuator endpoints: admin only
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // 5. Internal calls from other services: a valid token is required
                        .requestMatchers("/internal/**").authenticated()
                        // 6. Everything else: any logged-in user
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
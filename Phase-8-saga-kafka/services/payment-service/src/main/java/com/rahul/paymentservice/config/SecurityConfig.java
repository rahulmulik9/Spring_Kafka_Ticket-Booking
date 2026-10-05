package com.rahul.paymentservice.config;

import com.rahul.paymentservice.security.JwtAccessDeniedHandler;
import com.rahul.paymentservice.security.JwtAuthenticationEntryPoint;
import com.rahul.paymentservice.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
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
                        // 1. Public: only the health check
                        .requestMatchers("/actuator/health").permitAll()
                        // 2. Other actuator endpoints: admin only
                        .requestMatchers("/actuator/**").hasRole("ADMIN")
                        // 3. Everything else (all payment calls): any logged-in user
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
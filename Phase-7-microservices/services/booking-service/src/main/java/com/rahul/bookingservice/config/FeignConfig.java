package com.rahul.bookingservice.config;

import com.rahul.bookingservice.client.FeignErrorDecoder;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    // Every outgoing call carries the same token the customer sent us,
    // so cinema, payment and notification know who is behind the request.
    @Bean
    public RequestInterceptor tokenForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String token = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
                if (token != null) {
                    template.header(HttpHeaders.AUTHORIZATION, token);
                }
            }
        };
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }
}
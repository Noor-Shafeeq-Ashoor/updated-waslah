package com.ga.identity_service.security;

import com.ga.identity_service.security.RateLimitingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final RateLimitingInterceptor rateLimitingInterceptor;

    @Override
    public void addInterceptors(
            InterceptorRegistry registry
    ) {

        registry.addInterceptor(rateLimitingInterceptor)
                .addPathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/auth/forgot-password",
                        "/auth/reset-password"
                );
    }
}
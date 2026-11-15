package com.ga.notification_service.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ga.workspace_service.security.InternalApiFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final InternalApiFilter internalApiFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();


    // No users table here: stops Spring Boot from creating a default in-memory user.
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers(
                                "/actuator/health/**"
                        ).permitAll()

                        .requestMatchers(
                                "/api/internal/**"
                        ).hasRole("INTERNAL")

                        .anyRequest().authenticated()
                )

                .exceptionHandling(exception -> exception

                        // No JWT / invalid authentication
                        .authenticationEntryPoint(
                                (request, response, authException) -> {

                                    response.setStatus(HttpStatus.UNAUTHORIZED.value());

                                    response.setContentType("application/json");

                                    Map<String, Object> error =
                                            new HashMap<>();

                                    error.put("status", HttpStatus.UNAUTHORIZED.value());

                                    error.put("error", "UNAUTHORIZED");

                                    error.put("message", "Authentication is required");

                                    error.put("path", request.getRequestURI());

                                    objectMapper.writeValue(
                                            response.getOutputStream(),
                                            error
                                    );
                                }
                        )

                        // Authenticated but not allowed
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) -> {

                                    response.setStatus(HttpStatus.FORBIDDEN.value());

                                    response.setContentType("application/json");

                                    Map<String, Object> error = new HashMap<>();

                                    error.put("status", HttpStatus.FORBIDDEN.value());

                                    error.put("error", "FORBIDDEN");

                                    error.put("message", "You do not have permission to access this resource");

                                    error.put("path", request.getRequestURI());

                                    objectMapper.writeValue( response.getOutputStream(), error);
                                }
                        )
                )

                .addFilterBefore(
                        internalApiFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}


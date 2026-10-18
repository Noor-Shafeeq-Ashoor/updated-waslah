package com.ga.waslah.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates requests that contain a Bearer JWT.
 * Invalid or expired tokens are rejected with HTTP 401.
 * Requests without a Bearer token continue through the security chain,
 * allowing Spring Security to apply the rules for each endpoint.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final MyUserDetailsService userDetailsService;

    /**
     * Extracts and validates the JWT before setting the security context.
     *
     * @param request incoming HTTP request
     * @param response HTTP response
     * @param filterChain chain of security filters
     * @throws ServletException if a servlet-related error occurs
     * @throws IOException if an input/output error occurs
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Leave authentication decisions to Spring Security when no Bearer
        // token is provided. This allows public endpoints to remain accessible.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            // Parsing may throw an exception for malformed or expired tokens.
            String username = jwtService.extractUsername(token);

            if (username == null) {
                rejectInvalidToken(response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(username);

                if (!jwtService.isTokenValid(token, userDetails)) {
                    rejectInvalidToken(response);
                    return;
                }

                // Set the authenticated user and their granted authorities.
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

        } catch (JwtException | IllegalArgumentException exception) {
            // Do not log the token or expose parsing details to the client.
            log.warn(
                    "JWT rejected: {}",
                    exception.getClass().getSimpleName()
            );

            rejectInvalidToken(response);
            return;

        } catch (UsernameNotFoundException exception) {
            log.warn("JWT rejected because its user account was not found");

            rejectInvalidToken(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Clears any existing authentication and returns a generic 401 response.
     *
     * @param response HTTP response to update
     * @throws IOException if writing the response fails
     */
    private void rejectInvalidToken(
            HttpServletResponse response
    ) throws IOException {

        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                """
                {
                    "status": 401,
                    "error": "UNAUTHORIZED",
                    "message": "Invalid or expired authentication token."
                }
                """
        );
    }
}

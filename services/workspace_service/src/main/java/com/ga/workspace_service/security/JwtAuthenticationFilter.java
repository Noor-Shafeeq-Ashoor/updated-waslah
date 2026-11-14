package com.ga.workspace_service.security;

import com.ga.workspace_service.model.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

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

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

       if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.extractAllClaims(token);

            Number uid = claims.get("uid", Number.class);
            String username = claims.getSubject();
            String role = claims.get("role", String.class);

            if (uid == null || username == null || role == null) {
                log.warn("JWT rejected: missing uid, subject or role claim");
                rejectInvalidToken(response);
                return;
            }

            JwtPrincipal principal = new JwtPrincipal(uid.longValue(), username, Role.valueOf(role));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()))
                    );
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException | IllegalArgumentException exception) {
            log.warn("JWT rejected: {}", exception.getClass().getSimpleName());
            rejectInvalidToken(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    //Clears any existing authentication and returns a generic 401 response.

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

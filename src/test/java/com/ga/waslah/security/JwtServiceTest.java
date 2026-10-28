package com.ga.waslah.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtServiceTest {

    private static final String SECRET =
            "test-only-secret-key-that-is-at-least-32-chars-long";

    private final JwtService jwtService = new JwtService(SECRET);

    private Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @Test
    void tokenCarriesTheRoleClaimForTheFrontend() {
        String token = jwtService.generateToken("noor", "CAFE_MANAGER");

        assertEquals("noor", jwtService.extractUsername(token));
        assertEquals("CAFE_MANAGER", parse(token).get("role", String.class));
    }

    @Test
    void tokenWithoutRoleStillWorks() {
        String token = jwtService.generateToken("noor");

        assertEquals("noor", jwtService.extractUsername(token));
        assertNull(parse(token).get("role"));
    }
}

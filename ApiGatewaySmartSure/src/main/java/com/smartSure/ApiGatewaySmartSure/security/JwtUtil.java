package com.smartSure.ApiGatewaySmartSure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    //  Secret key used to SIGN and VERIFY JWT
    // Comes from application.properties
    @Value("${jwt.secret}")
    private String secret;

    //  Token expiration time (in milliseconds)
    @Value("${jwt.expiration}")
    private Long expiration;

    // =========================================================
    //  CREATE SECURE SIGNING KEY
    // =========================================================
    private Key getSigningKey() {

        // Converts string secret → byte[] → cryptographic key
        // UTF-8 ensures consistent encoding across systems
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // =========================================================
    //  GENERATE JWT TOKEN
    // =========================================================
    public String generateToken(Long userId, String role) {

        return Jwts.builder()

                //  Subject = unique identity of user (userId)
                // We use userId instead of email → stable + faster lookup
                .setSubject(String.valueOf(userId))

                // 🏷 Custom claim → role (ADMIN / CUSTOMER)
                .claim("role", role)

                //  Token creation time
                .setIssuedAt(new Date())

                //  Expiration time
                .setExpiration(new Date(System.currentTimeMillis() + expiration))

                //  SIGN TOKEN (VERY IMPORTANT)
                // Without this → token is NOT secure
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)

                //  Final JWT string
                .compact();
    }

    // =========================================================
    //  EXTRACT ALL CLAIMS FROM TOKEN
    // =========================================================
    private Claims extractAllClaims(String token) {

        return Jwts.parserBuilder()

                //  Verify token using same signing key
                .setSigningKey(getSigningKey())

                // Build parser
                .build()

                // Parse token → validate signature + expiration
                .parseClaimsJws(token)

                // Extract payload (claims)
                .getBody();
    }

    // =========================================================
    //  EXTRACT USER ID (STRING)
    // =========================================================
    public String extractUserId(String token) {
        return extractAllClaims(token).getSubject();
    }

    // =========================================================
    //  EXTRACT USER ID (LONG)
    // =========================================================
    public Long extractUserIdAsLong(String token) {
        return Long.parseLong(extractUserId(token));
    }

    // =========================================================
    // EXTRACT ROLE
    // =========================================================
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    // =========================================================
    //  VALIDATE TOKEN
    // =========================================================
    public boolean validateToken(String token) {

        try {
            // If parsing succeeds → token is valid
            extractAllClaims(token);
            return true;

        } catch (JwtException | IllegalArgumentException e) {

            // Invalid token (expired / tampered / wrong signature)
            return false;
        }
    }
}
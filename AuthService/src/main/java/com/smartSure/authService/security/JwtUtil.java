package com.smartSure.authService.security;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
	
	@Value("${jwt.secret}")
	private String secret;
	
	@Value("${jwt.expiration}")
	private Long expiration;
	
	private Key getSigningKey() {
		return Keys.hmacShaKeyFor(secret.getBytes());
	}
	
//	public String generateToken(String email, String role) {
//		return Jwts.builder().subject(email).claim("role", role)
//				.issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expiration))
//				.signWith(getSigningKey()).compact();
//	}
	
	public String generateToken(Long userId, String role) {
		return Jwts.builder().subject(String.valueOf(userId)).claim("role", role)
				.issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + expiration))
				.signWith(getSigningKey()).compact();
	}
	
	private Claims getClaims(String token) {
		return Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes()))
				.build().parseSignedClaims(token).getPayload();
	}
	
//	public String extractEmail(String token) {
//		return getClaims(token).getSubject();
//	}
	
	public String extractUserId(String token) {
		return getClaims(token).getSubject();
	}
	
	public Long extractUserIdAsLong(String token) {
	    return Long.parseLong(getClaims(token).getSubject());
	}
	
	public String extractRole(String token) {
		return getClaims(token).get("role", String.class);
	}
	
	public boolean validateToken(String token) {
		try {
			getClaims(token);
			return true;
		}catch(JwtException e) {
			return false;
		}
	}
}

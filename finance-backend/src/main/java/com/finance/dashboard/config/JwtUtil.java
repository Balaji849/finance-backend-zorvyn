package com.finance.dashboard.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * Generate a signed JWT containing the user's email and role.
     */
    public String generate(String email, String role) {
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key())
                .compact();
    }

    /**
     * Parse and validate a token. Throws JwtException if invalid or expired.
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract the email (subject) from a token without throwing on failure.
     */
    public String extractEmail(String token) {
        try {
            return parse(token).getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    /**
     * Return true if the token is valid and not expired.
     */
    public boolean isValid(String token) {
        try {
            Claims claims = parse(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException e) {
            return false;
        }
    }
}

package com.neueda.transaction_monitor.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtTokenService {

    @Value("${hawk.jwt.secret}")
    private String secret;

    @Value("${hawk.jwt.expiry-hours:24}")
    private long expiryHours;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Issue a signed JWT for the given username and role. */
    public String generateToken(String username, String role, String fullName) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expiryHours * 3_600_000L);
        return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .claim("name", fullName != null ? fullName : username)
            .issuedAt(now)
            .expiration(exp)
            .signWith(getKey())
            .compact();
    }

    /** Returns true if the token signature and expiry are valid. */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(getKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}


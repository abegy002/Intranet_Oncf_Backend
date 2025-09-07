// src/main/java/.../security/JwtUtil.java
package com.example.intranet_back_stage.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // Access token validity: 1 day
    private final String accessSecret = "ACCESS_SECRET_CHANGE_ME_32+CHARS_LONG_2025";
    private final long accessExpirationMs = 86_400_000L; // 24h = 1 day

    // Long-lived refresh token (e.g., 7 days)
    private final String refreshSecret = "REFRESH_SECRET_CHANGE_ME_32+CHARS_LONG_2025";
    private final long refreshExpirationMs = 7 * 24 * 60 * 60_000;

    private Key key(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ==== ACCESS ====
    public String generateAccessToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(key(accessSecret), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key(accessSecret)).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ==== REFRESH ====
    public String generateRefreshToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(key(refreshSecret), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key(refreshSecret)).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUsernameFromAccess(String token) {
        return Jwts.parserBuilder().setSigningKey(key(accessSecret)).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public String extractUsernameFromRefresh(String token) {
        return Jwts.parserBuilder().setSigningKey(key(refreshSecret)).build()
                .parseClaimsJws(token).getBody().getSubject();
    }
}

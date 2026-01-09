package com.example.jwt.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${jwt4.app.accessSecret}")
    private String accessSecret;

    @Value("${jwt4.app.refreshSecret}")
    private String refreshSecret;

    @Value("${jwt4.app.jwtExpirationMs}")
    private long accessExpirationMs;

    @Getter
    @Value("${jwt4.app.refreshExpirationMs}")
    private long refreshExpirationMs;

    private Key accessKey;
    private Key refreshKey;

    @PostConstruct
    public void init() {
        accessKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        refreshKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
    }

    /* ================= ACCESS TOKEN ================= */

    public String generateAccessToken(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("type", "ACCESS")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpirationMs))
                .signWith(accessKey)
                .compact();
    }

    public String extractEmailFromAccessToken(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) accessKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isAccessToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith((SecretKey) accessKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return "ACCESS".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith((SecretKey) refreshKey) // ✅ REFRESH KEY
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return "REFRESH".equals(claims.get("type"));
        } catch (Exception e) {
            return false;
        }
    }


    /* ================= REFRESH TOKEN ================= */

    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "REFRESH")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(refreshKey)
                .compact();
    }

    public UUID extractUserIdFromRefreshToken(String token) {
        return UUID.fromString(
                Jwts.parser()
                        .verifyWith((SecretKey) refreshKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject()
        );
    }

    public String extractEmailIgnoringExpiration(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) refreshKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

}

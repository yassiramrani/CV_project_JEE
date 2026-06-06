package org.example.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.example.model.Role;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {

    // 256-bit secret key for HMAC-SHA256 (in production, use environment variables)
    private static final String SECRET = "my-32-character-ultra-secure-and-ultra-long-secret";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());
    private static final long EXPIRATION_TIME = 864_000_000; // 10 days

    public static String generateToken(Long userId, String email, Role role) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(KEY)
                .compact();
    }

    public static Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

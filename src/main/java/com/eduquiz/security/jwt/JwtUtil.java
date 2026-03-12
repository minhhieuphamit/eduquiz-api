package com.eduquiz.security.jwt;

import com.eduquiz.feature.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration; // milliseconds

    /**
     * Generate JWT access token cho user.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract email (subject) từ token.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract thời gian phát hành token.
     */
    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    /**
     * Kiểm tra token có hợp lệ không (đúng user + chưa hết hạn).
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Kiểm tra token có hợp lệ không, bao gồm check tokenInvalidatedAt (logout).
     * Token phải được phát hành SAU thời điểm invalidate mới hợp lệ.
     */
    public boolean isTokenValid(String token, User user) {
        final String email = extractEmail(token);
        if (!email.equals(user.getUsername()) || isTokenExpired(token)) {
            return false;
        }

        // Check token invalidation (logout)
        if (user.getTokenInvalidatedAt() != null) {
            Date issuedAt = extractIssuedAt(token);
            LocalDateTime tokenIssuedAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(issuedAt.getTime()), ZoneId.systemDefault());
            // Token phải được phát hành SAU thời điểm invalidate
            if (!tokenIssuedAt.isAfter(user.getTokenInvalidatedAt())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Lấy thời gian hết hạn (seconds) để trả về client.
     */
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration / 1000;
    }

    // ── Private helpers ──

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

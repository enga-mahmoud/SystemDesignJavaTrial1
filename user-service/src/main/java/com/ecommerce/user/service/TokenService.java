package com.ecommerce.user.service;

import com.ecommerce.user.dto.TokenResponse;
import com.ecommerce.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class TokenService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.secret:myVerySecretKeyForDevelopmentPurposesOnly1234567890}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiry-ms:900000}")
    private long accessTokenExpiryMs;

    @Value("${jwt.refresh-token-expiry-s:604800}")
    private long refreshTokenExpiryS;

    public TokenService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            return Keys.hmacShaKeyFor(paddedKey);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public TokenResponse issueTokens(User user) {
        String jti = UUID.randomUUID().toString();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiryMs);

        String accessToken = Jwts.builder()
            .subject(user.getId().toString())
            .claim("email", user.getEmail())
            .claim("role", user.getRole().name())
            .id(jti)
            .issuedAt(now)
            .expiration(expiry)
            .issuer("ecommerce-platform")
            .signWith(getSigningKey())
            .compact();

        String refreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
            "refresh:" + user.getId(),
            refreshToken,
            refreshTokenExpiryS,
            TimeUnit.SECONDS
        );

        return new TokenResponse(accessToken, refreshToken, accessTokenExpiryMs / 1000);
    }

    public boolean validateRefreshToken(String userId, String providedToken) {
        String stored = redisTemplate.opsForValue().get("refresh:" + userId);
        return stored != null && stored.equals(providedToken);
    }

    public void revokeRefreshToken(String userId) {
        redisTemplate.delete("refresh:" + userId);
    }

    public void blacklistToken(String jti, long remainingTtlMs) {
        if (remainingTtlMs > 0) {
            redisTemplate.opsForValue().set(
                "blacklist:" + jti,
                "1",
                remainingTtlMs,
                TimeUnit.MILLISECONDS
            );
        }
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}

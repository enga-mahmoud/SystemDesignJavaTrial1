package com.ecommerce.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.public-key:}")
    private String publicKeyPem;

    @Value("${jwt.secret:myVerySecretKeyForDevelopmentPurposesOnly1234567890}")
    private String jwtSecret;

    private PublicKey publicKey;
    private SecretKey hmacKey;

    @PostConstruct
    public void init() {
        if (publicKeyPem != null && !publicKeyPem.isBlank() && !publicKeyPem.contains("DUMMY")) {
            try {
                String cleaned = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
                byte[] keyBytes = Base64.getDecoder().decode(cleaned);
                KeyFactory kf = KeyFactory.getInstance("RSA");
                publicKey = kf.generatePublic(new X509EncodedKeySpec(keyBytes));
                return;
            } catch (Exception e) {
                // Fail loudly — a misconfigured RSA key is a deployment error, not a recoverable condition
                throw new IllegalStateException("Failed to load JWT public key; check jwt.public-key config", e);
            }
        }
        log.warn("No RSA public key configured — falling back to HMAC. Set jwt.public-key for production.");
        // Fall back to HMAC — must match the secret used in user-service TokenService
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        hmacKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public Claims validateAndParse(String token) throws JwtException {
        if (publicKey != null) {
            return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        }
        return Jwts.parser()
            .verifyWith(hmacKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}

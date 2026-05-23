package com.ecommerce.user.service;

import com.ecommerce.user.dto.LoginRequest;
import com.ecommerce.user.dto.RefreshRequest;
import com.ecommerce.user.dto.RegisterRequest;
import com.ecommerce.user.dto.TokenResponse;
import com.ecommerce.user.entity.User;
import com.ecommerce.user.enums.Role;
import com.ecommerce.user.kafka.UserEventPublisher;
import com.ecommerce.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserEventPublisher eventPublisher;

    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(Role.USER)
            .build();

        user = userRepository.save(user);
        eventPublisher.publishUserRegistered(user);
        return tokenService.issueTokens(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        return tokenService.issueTokens(user);
    }

    public TokenResponse refresh(RefreshRequest request) {
        if (!tokenService.validateRefreshToken(request.getUserId(), request.getRefreshToken())) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        User user = userRepository.findById(java.util.UUID.fromString(request.getUserId()))
            .orElseThrow(() -> new RuntimeException("User not found"));

        tokenService.revokeRefreshToken(request.getUserId());
        return tokenService.issueTokens(user);
    }

    public void logout(String token) {
        try {
            Claims claims = tokenService.parseToken(token);
            String userId = claims.getSubject();
            String jti = claims.getId();
            long remainingTtl = claims.getExpiration().getTime() - new Date().getTime();

            tokenService.revokeRefreshToken(userId);
            tokenService.blacklistToken(jti, remainingTtl);
        } catch (Exception ignored) {
            // Token already invalid or expired — logout succeeds anyway
        }
    }
}

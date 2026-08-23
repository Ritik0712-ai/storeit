package com.cloudvault.service;

import com.cloudvault.dto.AuthDTO;
import com.cloudvault.entity.RefreshToken;
import com.cloudvault.entity.User;
import com.cloudvault.repository.RefreshTokenRepository;
import com.cloudvault.repository.UserRepository;
import com.cloudvault.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthDTO.AuthResponse register(AuthDTO.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already in use");
        }

        User user = User.builder()
                .email(request.getEmail())
                .displayName(request.getDisplayName())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .authProvider(User.AuthProvider.local)
                .build();

        user = userRepository.save(user);

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());

        return new AuthDTO.AuthResponse(
                accessToken,
                toUserResponse(user)
        );
    }

    @Transactional
    public AuthDTO.AuthResponse login(AuthDTO.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());

        return new AuthDTO.AuthResponse(
                accessToken,
                toUserResponse(user)
        );
    }

    @Transactional
    public void logout(String token) {
        String tokenHash = hashToken(token);
        refreshTokenRepository.revokeByTokenHash(tokenHash);
    }

    @Transactional
    public AuthDTO.RefreshResponse refresh(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired");
        }

        User user = storedToken.getUser();
        String newAccessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());

        return new AuthDTO.RefreshResponse(newAccessToken);
    }

    public AuthDTO.UserResponse getCurrentUser(User user) {
        return toUserResponse(user);
    }

    private AuthDTO.UserResponse toUserResponse(User user) {
        return new AuthDTO.UserResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAuthProvider().name().toLowerCase(),
                user.getCreatedAt().toString()
        );
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}

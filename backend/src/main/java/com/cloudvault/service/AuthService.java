package com.cloudvault.service;

import com.cloudvault.dto.AuthDTO;
import com.cloudvault.entity.RefreshToken;
import com.cloudvault.entity.User;
import com.cloudvault.repository.RefreshTokenRepository;
import com.cloudvault.repository.UserRepository;
import com.cloudvault.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USERINFO_URI = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

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

        user = userRepository.saveAndFlush(user);

        return issueTokens(user);
    }

    @Transactional
    public AuthDTO.AuthResponse login(AuthDTO.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthDTO.AuthResponse loginWithGoogle(String code, String redirectUri) {
        MultiValueMap<String, String> tokenRequestBody = new LinkedMultiValueMap<>();
        tokenRequestBody.add("code", code);
        tokenRequestBody.add("client_id", googleClientId);
        tokenRequestBody.add("client_secret", googleClientSecret);
        tokenRequestBody.add("redirect_uri", redirectUri);
        tokenRequestBody.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        Map<String, Object> tokenResponse;
        try {
            tokenResponse = restTemplate.postForObject(
                    GOOGLE_TOKEN_URI,
                    new HttpEntity<>(tokenRequestBody, headers),
                    Map.class
            );
        } catch (RestClientException e) {
            throw new IllegalArgumentException("Failed to exchange Google authorization code: " + e.getMessage(), e);
        }

        if (tokenResponse == null || tokenResponse.get("access_token") == null) {
            throw new IllegalArgumentException("Google did not return an access token");
        }
        String googleAccessToken = (String) tokenResponse.get("access_token");

        HttpHeaders userInfoHeaders = new HttpHeaders();
        userInfoHeaders.setBearerAuth(googleAccessToken);

        Map<String, Object> userInfo;
        try {
            userInfo = restTemplate.exchange(
                    GOOGLE_USERINFO_URI,
                    org.springframework.http.HttpMethod.GET,
                    new HttpEntity<>(userInfoHeaders),
                    Map.class
            ).getBody();
        } catch (RestClientException e) {
            throw new IllegalArgumentException("Failed to fetch Google user info: " + e.getMessage(), e);
        }

        if (userInfo == null || userInfo.get("email") == null) {
            throw new IllegalArgumentException("Google did not return an email address");
        }

        String email = (String) userInfo.get("email");
        String name = userInfo.get("name") != null ? (String) userInfo.get("name") : email;
        String picture = (String) userInfo.get("picture");

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = User.builder()
                    .email(email)
                    .displayName(name)
                    .authProvider(User.AuthProvider.google)
                    .profilePictureUrl(picture)
                    .build();
            user = userRepository.saveAndFlush(user);
        } else if (user.getProfilePictureUrl() == null && picture != null) {
            user.setProfilePictureUrl(picture);
            user = userRepository.save(user);
        }

        return issueTokens(user);
    }

    private AuthDTO.AuthResponse issueTokens(User user) {
        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "");

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 60 * 60))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthDTO.AuthResponse(
                accessToken,
                refreshToken,
                toUserResponse(user)
        );
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
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

    @Transactional
    public AuthDTO.UserResponse updateProfile(UUID userId, AuthDTO.UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getMobileNumber() != null) {
            user.setMobileNumber(request.getMobileNumber());
        }
        if (request.getProfilePictureUrl() != null) {
            user.setProfilePictureUrl(request.getProfilePictureUrl());
        }
        
        user = userRepository.save(user);
        return toUserResponse(user);
    }

    private AuthDTO.UserResponse toUserResponse(User user) {
        return new AuthDTO.UserResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAuthProvider().name().toLowerCase(),
                user.getProfilePictureUrl(),
                user.getMobileNumber(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : Instant.now().toString()
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

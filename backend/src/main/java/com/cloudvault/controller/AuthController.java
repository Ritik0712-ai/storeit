package com.cloudvault.controller;

import com.cloudvault.dto.AuthDTO;
import com.cloudvault.entity.User;
import com.cloudvault.service.AuthService;
import com.cloudvault.service.ProfilePictureService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ProfilePictureService profilePictureService;

    @PostMapping("/register")
    public ResponseEntity<AuthDTO.AuthResponse> register(
            @Valid @RequestBody AuthDTO.RegisterRequest request,
            HttpServletResponse response) {
        AuthDTO.AuthResponse authResponse = authService.register(request);

        // Set refresh token as HttpOnly cookie
        Cookie cookie = new Cookie("refresh_token", authResponse.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(cookie);

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDTO.AuthResponse> login(
            @Valid @RequestBody AuthDTO.LoginRequest request,
            HttpServletResponse response) {
        AuthDTO.AuthResponse authResponse = authService.login(request);

        // Set refresh token as HttpOnly cookie
        Cookie cookie = new Cookie("refresh_token", authResponse.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(cookie);

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletResponse response,
            @AuthenticationPrincipal User user) {
        if (user != null) {
            authService.logout(user.getId());
        }

        // Clear refresh token cookie
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthDTO.RefreshResponse> refresh(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String refreshToken = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }

        AuthDTO.RefreshResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthDTO.UserResponse> getMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.getCurrentUser(user));
    }

    @PostMapping("/oauth2/callback")
    public ResponseEntity<AuthDTO.AuthResponse> oauthCallback(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        // Note: Full OAuth callback handler would exchange code for tokens via Google
        // This is a placeholder; the actual implementation requires Google token exchange
        AuthDTO.UserResponse userResp = new AuthDTO.UserResponse(
                "google-user-id", "user@example.com", "Google User", "google", null, null, Instant.now().toString());
        AuthDTO.AuthResponse response = new AuthDTO.AuthResponse(
                "temp-access-token", "temp-refresh-token", userResp);

        // Set refresh token as HttpOnly cookie
        Cookie cookie = new Cookie("refresh_token", response.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge(7 * 24 * 60 * 60);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/profile")
    public ResponseEntity<AuthDTO.UserResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody AuthDTO.UpdateProfileRequest request) {
        AuthDTO.UserResponse response = authService.updateProfile(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/profile/picture")
    public ResponseEntity<Map<String, String>> uploadProfilePicture(
            @AuthenticationPrincipal User user,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        String publicUrl = profilePictureService.uploadProfilePicture(user.getId(), file);
        return ResponseEntity.ok(Map.of("profilePictureUrl", publicUrl));
    }
}

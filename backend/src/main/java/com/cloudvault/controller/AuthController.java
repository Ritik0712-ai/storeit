package com.cloudvault.controller;

import com.cloudvault.dto.AuthDTO;
import com.cloudvault.entity.User;
import com.cloudvault.service.AuthService;
import com.cloudvault.service.ProfilePictureService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ProfilePictureService profilePictureService;

    // Frontend and backend live on different domains (vercel.app / onrender.com),
    // so this cookie is cross-site: it must be SameSite=None + Secure or the
    // browser silently drops it and /auth/refresh never receives it.
    private ResponseCookie buildRefreshCookie(String value, int maxAgeSeconds) {
        return ResponseCookie.from("refresh_token", value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/v1/auth/refresh")
                .maxAge(maxAgeSeconds)
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<AuthDTO.AuthResponse> register(@Valid @RequestBody AuthDTO.RegisterRequest request) {
        AuthDTO.AuthResponse authResponse = authService.register(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(authResponse.getRefreshToken(), 7 * 24 * 60 * 60).toString())
                .body(authResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthDTO.AuthResponse> login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        AuthDTO.AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(authResponse.getRefreshToken(), 7 * 24 * 60 * 60).toString())
                .body(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal User user) {
        if (user != null) {
            authService.logout(user.getId());
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie("", 0).toString())
                .build();
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
    public ResponseEntity<AuthDTO.AuthResponse> oauthCallback(@Valid @RequestBody AuthDTO.OAuth2CallbackRequest request) {
        AuthDTO.AuthResponse authResponse = authService.loginWithGoogle(request.getCode(), request.getRedirectUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(authResponse.getRefreshToken(), 7 * 24 * 60 * 60).toString())
                .body(authResponse);
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

package com.cloudvault.controller;

import com.cloudvault.dto.AuthDTO;
import com.cloudvault.entity.User;
import com.cloudvault.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthDTO.AuthResponse> register(
            @Valid @RequestBody AuthDTO.RegisterRequest request,
            HttpServletResponse response) {
        AuthDTO.AuthResponse authResponse = authService.register(request);

        // Set refresh token as HttpOnly cookie
        Cookie cookie = new Cookie("refresh_token", authResponse.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
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
        cookie.setSecure(true);
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
        cookie.setSecure(true);
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
        return ResponseEntity.ok(AuthDTO.UserResponse.from(user));
    }

    @PostMapping("/oauth2/callback")
    public ResponseEntity<AuthDTO.AuthResponse> oauthCallback(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        AuthDTO.AuthResponse response = authService.handleOAuthCallback(code);

        // Set refresh token as HttpOnly cookie
        Cookie cookie = new Cookie("refresh_token", response.getRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setMaxAge(7 * 24 * 60 * 60);

        return ResponseEntity.ok(response);
    }
}

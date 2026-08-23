package com.cloudvault.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDTO {

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Display name is required")
        @Size(min = 2, max = 100, message = "Display name must be 2-100 characters")
        private String displayName;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class AuthResponse {
        private String accessToken;
        private String refreshToken;
        private UserResponse user;

        public AuthResponse(String accessToken, String refreshToken, UserResponse user) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.user = user;
        }
    }

    @Data
    public static class UserResponse {
        private String id;
        private String email;
        private String displayName;
        private String authProvider;
        private String createdAt;

        public UserResponse(String id, String email, String displayName, String authProvider, String createdAt) {
            this.id = id;
            this.email = email;
            this.displayName = displayName;
            this.authProvider = authProvider;
            this.createdAt = createdAt;
        }
    }

    @Data
    public static class RefreshResponse {
        private String accessToken;

        public RefreshResponse(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    @Data
    public static class OAuth2CallbackRequest {
        private String code;
    }
}

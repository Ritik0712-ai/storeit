package com.cloudvault.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ShareDTO {

    @Data
    public static class CreateShareRequest {
        @NotBlank(message = "Resource type is required")
        @Pattern(regexp = "^(file|folder)$", message = "Resource type must be 'file' or 'folder'")
        private String resourceType;

        @NotNull(message = "Resource ID is required")
        private UUID resourceId;

        @NotBlank(message = "Target user email is required")
        @Email(message = "Invalid email format")
        private String targetUserEmail;

        @NotBlank(message = "Role is required")
        @Pattern(regexp = "^(viewer|editor)$", message = "Role must be 'viewer' or 'editor'")
        private String role;
    }

    @Data
    public static class ShareResponse {
        private UUID id;
        private String resourceType;
        private UUID resourceId;
        private UUID sharedById;
        private String sharedByName;
        private UUID sharedWithId;
        private String sharedWithEmail;
        private String role;
        private String createdAt;

        public static ShareResponse from(com.cloudvault.entity.Share share) {
            ShareResponse response = new ShareResponse();
            response.setId(share.getId());
            response.setResourceType(share.getResourceType().name().toLowerCase());
            response.setResourceId(share.getResourceId());
            response.setSharedById(share.getSharedBy().getId());
            response.setSharedByName(share.getSharedBy().getDisplayName());
            response.setSharedWithId(share.getSharedWith().getId());
            response.setSharedWithEmail(share.getSharedWith().getEmail());
            response.setRole(share.getRole().name().toLowerCase());
            response.setCreatedAt(share.getCreatedAt().toString());
            return response;
        }
    }

    @Data
    public static class SharedWithMeResponse {
        private List<SharedItem> items;

        @Data
        public static class SharedItem {
            private UUID id;
            private String resourceType;
            private UUID resourceId;
            private String resourceName;
            private UUID sharedById;
            private String sharedByName;
            private String role;
            private String createdAt;

            public SharedItem(UUID id, String resourceType, UUID resourceId, String resourceName,
                            UUID sharedById, String sharedByName, String role, String createdAt) {
                this.id = id;
                this.resourceType = resourceType;
                this.resourceId = resourceId;
                this.resourceName = resourceName;
                this.sharedById = sharedById;
                this.sharedByName = sharedByName;
                this.role = role;
                this.createdAt = createdAt;
            }
        }
    }

    @Data
    public static class CreatePublicLinkRequest {
        @NotBlank(message = "Resource type is required")
        @Pattern(regexp = "^(file|folder)$", message = "Resource type must be 'file' or 'folder'")
        private String resourceType;

        @NotNull(message = "Resource ID is required")
        private UUID resourceId;

        private String expiresAt;
        private String password;
    }

    @Data
    public static class PublicLinkResponse {
        private UUID id;
        private String token;
        private String resourceType;
        private UUID resourceId;
        private String expiresAt;
        private boolean hasPassword;
        private String createdAt;
        private String shareUrl;

        public static PublicLinkResponse from(com.cloudvault.entity.PublicLink link, String baseUrl) {
            PublicLinkResponse response = new PublicLinkResponse();
            response.setId(link.getId());
            response.setToken(link.getToken());
            response.setResourceType(link.getResourceType().name().toLowerCase());
            response.setResourceId(link.getResourceId());
            response.setExpiresAt(link.getExpiresAt() != null ? link.getExpiresAt().toString() : null);
            response.setHasPassword(link.getPasswordHash() != null);
            response.setCreatedAt(link.getCreatedAt().toString());
            response.setShareUrl(baseUrl + "/share/" + link.getToken());
            return response;
        }
    }

    @Data
    public static class PublicLinkInfoResponse {
        private String resourceType;
        private String resourceName;
        private String ownerName;
        private boolean requiresPassword;
        private boolean isExpired;
        private boolean isRevoked;
    }
}

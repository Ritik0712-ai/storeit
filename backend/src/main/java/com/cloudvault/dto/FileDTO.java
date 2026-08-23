package com.cloudvault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

public class FileDTO {

    @Data
    public static class InitUploadRequest {
        @NotBlank(message = "File name is required")
        @Size(max = 255, message = "File name must be at most 255 characters")
        private String fileName;

        @NotBlank(message = "MIME type is required")
        private String mimeType;

        @NotNull(message = "File size is required")
        @Positive(message = "File size must be positive")
        private Long sizeBytes;

        private UUID folderId;
    }

    @Data
    public static class InitUploadResponse {
        private UUID fileId;
        private String uploadUrl;
        private String storagePath;

        public InitUploadResponse(UUID fileId, String uploadUrl, String storagePath) {
            this.fileId = fileId;
            this.uploadUrl = uploadUrl;
            this.storagePath = storagePath;
        }
    }

    @Data
    public static class CompleteUploadRequest {
        private String checksum;
    }

    @Data
    public static class UpdateRequest {
        @Size(max = 255, message = "File name must be at most 255 characters")
        private String name;

        private UUID folderId;
    }

    @Data
    public static class FileResponse {
        private UUID id;
        private String name;
        private UUID folderId;
        private UUID ownerId;
        private String storagePath;
        private String mimeType;
        private Long sizeBytes;
        private String uploadStatus;
        private String deletedAt;
        private String createdAt;
        private String updatedAt;
        private Boolean isStarred;

        public static FileResponse from(com.cloudvault.entity.FileEntity file) {
            FileResponse response = new FileResponse();
            response.setId(file.getId());
            response.setName(file.getName());
            response.setFolderId(file.getFolder() != null ? file.getFolder().getId() : null);
            response.setOwnerId(file.getOwner().getId());
            response.setStoragePath(file.getStoragePath());
            response.setMimeType(file.getMimeType());
            response.setSizeBytes(file.getSizeBytes());
            response.setUploadStatus(file.getUploadStatus().name().toLowerCase());
            response.setDeletedAt(file.getDeletedAt() != null ? file.getDeletedAt().toString() : null);
            response.setCreatedAt(file.getCreatedAt().toString());
            response.setUpdatedAt(file.getUpdatedAt().toString());
            return response;
        }
    }

    @Data
    public static class DownloadUrlResponse {
        private String downloadUrl;

        public DownloadUrlResponse(String downloadUrl) {
            this.downloadUrl = downloadUrl;
        }
    }
}

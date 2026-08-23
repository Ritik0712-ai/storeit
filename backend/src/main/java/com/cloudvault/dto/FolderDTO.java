package com.cloudvault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class FolderDTO {

    @Data
    public static class CreateRequest {
        @NotBlank(message = "Folder name is required")
        @Size(max = 255, message = "Folder name must be at most 255 characters")
        private String name;

        private UUID parentFolderId;
    }

    @Data
    public static class UpdateRequest {
        @Size(max = 255, message = "Folder name must be at most 255 characters")
        private String name;

        private UUID parentFolderId;
    }

    @Data
    public static class FolderResponse {
        private UUID id;
        private String name;
        private UUID ownerId;
        private UUID parentFolderId;
        private String deletedAt;
        private String createdAt;
        private String updatedAt;
        private Boolean isStarred;

        public static FolderResponse from(com.cloudvault.entity.Folder folder) {
            FolderResponse response = new FolderResponse();
            response.setId(folder.getId());
            response.setName(folder.getName());
            response.setOwnerId(folder.getOwner().getId());
            response.setParentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getId() : null);
            response.setDeletedAt(folder.getDeletedAt() != null ? folder.getDeletedAt().toString() : null);
            response.setCreatedAt(folder.getCreatedAt().toString());
            response.setUpdatedAt(folder.getUpdatedAt().toString());
            return response;
        }
    }

    @Data
    public static class FolderWithChildrenResponse {
        private UUID id;
        private String name;
        private UUID ownerId;
        private UUID parentFolderId;
        private String createdAt;
        private String updatedAt;
        private List<Object> children;

        public static FolderWithChildrenResponse from(com.cloudvault.entity.Folder folder, List<Object> children) {
            FolderWithChildrenResponse response = new FolderWithChildrenResponse();
            response.setId(folder.getId());
            response.setName(folder.getName());
            response.setOwnerId(folder.getOwner().getId());
            response.setParentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getId() : null);
            response.setCreatedAt(folder.getCreatedAt().toString());
            response.setUpdatedAt(folder.getUpdatedAt().toString());
            response.setChildren(children);
            return response;
        }
    }
}

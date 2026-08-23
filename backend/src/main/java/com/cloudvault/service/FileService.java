package com.cloudvault.service;

import com.cloudvault.dto.FileDTO;
import com.cloudvault.entity.FileEntity;
import com.cloudvault.entity.Folder;
import com.cloudvault.entity.User;
import com.cloudvault.repository.FileRepository;
import com.cloudvault.repository.FolderRepository;
import com.cloudvault.repository.StarRepository;
import com.cloudvault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final StarRepository starRepository;
    private final UserRepository userRepository;
    private final PermissionService permissionService;
    private final StorageService storageService;

    @Transactional
    public FileDTO.InitUploadResponse initUpload(UUID userId, FileDTO.InitUploadRequest request) {
        // Validate folder access if uploading to a folder
        if (request.getFolderId() != null) {
            if (permissionService.checkFolderPermission(userId, request.getFolderId(), PermissionService.Permission.EDIT)
                    == PermissionService.Permission.NONE) {
                throw new IllegalStateException("Access denied to target folder");
            }
        }

        // Get owner user
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        FileEntity file = FileEntity.builder()
                .name(request.getFileName())
                .mimeType(request.getMimeType())
                .sizeBytes(request.getSizeBytes())
                .owner(owner)
                .uploadStatus(FileEntity.UploadStatus.PENDING)
                .storagePath("temp") // temporary dummy path
                .build();

        if (request.getFolderId() != null) {
            Folder folder = folderRepository.findById(request.getFolderId()).orElse(null);
            file.setFolder(folder);
        }

        file = fileRepository.save(file);
        
        file.setStoragePath(storageService.generateStoragePath(file.getId()));
        file = fileRepository.save(file);

        String uploadUrl = storageService.generateUploadUrl(file.getId());

        return new FileDTO.InitUploadResponse(file.getId(), uploadUrl, file.getStoragePath());
    }

    @Transactional
    public FileDTO.FileResponse completeUpload(UUID userId, UUID fileId, String checksum) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        // Verify ownership
        if (!file.getOwner().getId().equals(userId)) {
            throw new IllegalStateException("Access denied to file");
        }

        file.setUploadStatus(FileEntity.UploadStatus.COMPLETE);
        file = fileRepository.save(file);

        return toFileResponse(file, userId);
    }

    @Transactional(readOnly = true)
    public FileDTO.FileResponse getFile(UUID userId, UUID fileId) {
        if (permissionService.checkFilePermission(userId, fileId, PermissionService.Permission.VIEW)
                == PermissionService.Permission.NONE) {
            throw new IllegalStateException("Access denied to file");
        }

        FileEntity file = fileRepository.findByIdWithOwner(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        return toFileResponse(file, userId);
    }

    @Transactional(readOnly = true)
    public String getDownloadUrl(UUID userId, UUID fileId) {
        if (permissionService.checkFilePermission(userId, fileId, PermissionService.Permission.VIEW)
                == PermissionService.Permission.NONE) {
            throw new IllegalStateException("Access denied to file");
        }

        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        return storageService.generateDownloadUrl(fileId, file.getStoragePath());
    }

    @Transactional
    public FileDTO.FileResponse updateFile(UUID userId, UUID fileId, String name, UUID folderId) {
        if (permissionService.checkEditPermission(userId, com.cloudvault.entity.Share.ResourceType.FILE, fileId)
                == PermissionService.Permission.NONE) {
            throw new IllegalStateException("Access denied to file");
        }

        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        if (name != null && !name.isBlank()) {
            file.setName(name);
        }

        if (folderId != null) {
            Folder folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

            if (folder.isDeleted()) {
                throw new IllegalArgumentException("Cannot move file to a deleted folder");
            }

            // Check permission on target folder
            if (permissionService.checkFolderPermission(userId, folderId, PermissionService.Permission.EDIT)
                    == PermissionService.Permission.NONE) {
                throw new IllegalStateException("Access denied to target folder");
            }

            file.setFolder(folder);
        }

        file = fileRepository.save(file);
        return toFileResponse(file, userId);
    }

    @Transactional
    public void deleteFile(UUID userId, UUID fileId) {
        if (permissionService.checkEditPermission(userId, com.cloudvault.entity.Share.ResourceType.FILE, fileId)
                == PermissionService.Permission.NONE) {
            throw new IllegalStateException("Access denied to file");
        }

        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        file.setDeletedAt(Instant.now());
        fileRepository.save(file);
    }

    private FileDTO.FileResponse toFileResponse(FileEntity file, UUID userId) {
        FileDTO.FileResponse response = FileDTO.FileResponse.from(file);
        response.setIsStarred(starRepository.existsByUserIdAndResourceTypeAndResourceId(
                userId, com.cloudvault.entity.Share.ResourceType.FILE, file.getId()));
        return response;
    }

    @Transactional(readOnly = true)
    public Long getStorageUsed(UUID userId) {
        return fileRepository.calculateStorageUsedByOwnerId(userId);
    }
}

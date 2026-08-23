package com.cloudvault.service;

import com.cloudvault.dto.FolderDTO;
import com.cloudvault.entity.FileEntity;
import com.cloudvault.entity.Folder;
import com.cloudvault.entity.User;
import com.cloudvault.repository.FileRepository;
import com.cloudvault.repository.FolderRepository;
import com.cloudvault.repository.StarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final StarRepository starRepository;
    private final PermissionService permissionService;
    private final StorageService storageService;

    @Transactional
    public FolderDTO.FolderWithChildrenResponse createFolder(UUID userId, User user, String name, UUID parentFolderId) {
        Folder parentFolder = null;

        if (parentFolderId != null) {
            parentFolder = folderRepository.findById(parentFolderId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));

            // Check permission on parent folder
            if (permissionService.checkFolderPermission(userId, parentFolderId, PermissionService.Permission.VIEW)
                    == PermissionService.Permission.NONE) {
                throw new IllegalStateException("Access denied to parent folder");
            }

            // Can't create folder inside a deleted folder
            if (parentFolder.isDeleted()) {
                throw new IllegalArgumentException("Cannot create folder inside a deleted folder");
            }
        }

        Folder folder = Folder.builder()
                .name(name)
                .owner(user)
                .parentFolder(parentFolder)
                .build();

        folder = folderRepository.save(folder);

        return getFolderWithChildren(folder.getId(), userId);
    }

    @Transactional(readOnly = true)
    public FolderDTO.FolderWithChildrenResponse getFolder(UUID folderId, UUID userId) {
        if (folderId == null) {
            // Root folder - return user's root contents
            return getRootFolder(userId);
        }

        // Check permission
        if (permissionService.checkFolderPermission(userId, folderId, PermissionService.Permission.VIEW)
                == PermissionService.Permission.NONE) {
            throw new IllegalStateException("Access denied to folder");
        }

        return getFolderWithChildren(folderId, userId);
    }

    private FolderDTO.FolderWithChildrenResponse getRootFolder(UUID userId) {
        List<Folder> rootFolders = folderRepository.findRootFoldersByOwnerId(userId);
        List<FileEntity> rootFiles = fileRepository.findRootFilesByOwnerId(userId);

        FolderDTO.FolderWithChildrenResponse response = new FolderDTO.FolderWithChildrenResponse();
        response.setId(null);
        response.setName("My Drive");
        response.setOwnerId(userId);
        response.setParentFolderId(null);
        response.setCreatedAt(Instant.now().toString());
        response.setUpdatedAt(Instant.now().toString());

        List<Object> children = new ArrayList<>();
        for (Folder folder : rootFolders) {
            FolderDTO.FolderResponse folderResp = FolderDTO.FolderResponse.from(folder);
            folderResp.setIsStarred(starRepository.existsByUserIdAndResourceTypeAndResourceId(
                    userId, com.cloudvault.entity.Share.ResourceType.FOLDER, folder.getId()));
            children.add(folderResp);
        }
        for (FileEntity file : rootFiles) {
            children.add(FileDTO.FileResponse.from(file));
        }

        response.setChildren(children);
        return response;
    }

    private FolderDTO.FolderWithChildrenResponse getFolderWithChildren(UUID folderId, UUID userId) {
        Folder folder = folderRepository.findByIdWithOwner(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        if (folder.isDeleted()) {
            throw new IllegalArgumentException("Folder has been deleted");
        }

        List<Folder> childFolders = folderRepository.findChildrenByParentId(folderId);
        List<FileEntity> childFiles = fileRepository.findByFolderId(folderId);

        FolderDTO.FolderWithChildrenResponse response = new FolderDTO.FolderWithChildrenResponse();
        response.setId(folder.getId());
        response.setName(folder.getName());
        response.setOwnerId(folder.getOwner().getId());
        response.setParentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getId() : null);
        response.setCreatedAt(folder.getCreatedAt().toString());
        response.setUpdatedAt(folder.getUpdatedAt().toString());

        List<Object> children = new ArrayList<>();
        for (Folder child : childFolders) {
            FolderDTO.FolderResponse folderResp = FolderDTO.FolderResponse.from(child);
            folderResp.setIsStarred(starRepository.existsByUserIdAndResourceTypeAndResourceId(
                    userId, com.cloudvault.entity.Share.ResourceType.FOLDER, child.getId()));
            children.add(folderResp);
        }
        for (FileEntity file : childFiles) {
            children.add(FileDTO.FileResponse.from(file));
        }

        response.setChildren(children);
        return response;
    }

    @Transactional
    public FolderDTO.FolderResponse updateFolder(UUID userId, UUID folderId, String name, UUID parentFolderId) {
        // Check edit permission
        if (permissionService.checkEditPermission(userId, com.cloudvault.entity.Share.ResourceType.FOLDER, folderId)
                == PermissionService.Permission.NONE) {
            throw new IllegalStateException("Access denied to folder");
        }

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        if (name != null && !name.isBlank()) {
            folder.setName(name);
        }

        if (parentFolderId != null) {
            // Can't move to itself or its descendants
            if (parentFolderId.equals(folderId)) {
                throw new IllegalArgumentException("Cannot move folder into itself");
            }

            List<UUID> descendantIds = folderRepository.findAncestorIds(folderId);
            if (descendantIds.contains(parentFolderId)) {
                throw new IllegalArgumentException("Cannot move folder into its descendant");
            }

            Folder newParent = folderRepository.findById(parentFolderId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));

            if (newParent.isDeleted()) {
                throw new IllegalArgumentException("Cannot move folder into a deleted folder");
            }

            folder.setParentFolder(newParent);
        }

        folder = folderRepository.save(folder);
        return FolderDTO.FolderResponse.from(folder);
    }

    @Transactional
    public void deleteFolder(UUID userId, UUID folderId) {
        // Check edit permission
        if (permissionService.checkEditPermission(userId, com.cloudvault.entity.Share.ResourceType.FOLDER, folderId)
                == PermissionService.Permission.NONE) {
            throw new IllegalStateException("Access denied to folder");
        }

        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        // Soft delete the folder and all descendants
        softDeleteFolderAndDescendants(folder);

        // Soft delete all files in this folder
        softDeleteFilesInFolder(folderId);
    }

    private void softDeleteFolderAndDescendants(Folder folder) {
        folder.setDeletedAt(Instant.now());
        folderRepository.save(folder);

        // Recursively handle children
        for (Folder child : folderRepository.findChildrenByParentId(folder.getId())) {
            softDeleteFolderAndDescendants(child);
        }
    }

    private void softDeleteFilesInFolder(UUID folderId) {
        List<FileEntity> files = fileRepository.findByFolderId(folderId);
        for (FileEntity file : files) {
            file.setDeletedAt(Instant.now());
            fileRepository.save(file);
        }
    }

    // Inner DTO import
    private static class FileDTO {
        static com.cloudvault.dto.FileDTO.FileResponse from(FileEntity file) {
            return com.cloudvault.dto.FileDTO.FileResponse.from(file);
        }
    }
}

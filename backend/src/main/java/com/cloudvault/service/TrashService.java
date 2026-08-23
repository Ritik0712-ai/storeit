package com.cloudvault.service;

import com.cloudvault.dto.FolderDTO;
import com.cloudvault.dto.SearchDTO;
import com.cloudvault.entity.*;
import com.cloudvault.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TrashService {

    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final StarRepository starRepository;
    private final PermissionService permissionService;

    @Transactional(readOnly = true)
    public List<SearchDTO.TrashItem> getTrash(UUID userId) {
        List<SearchDTO.TrashItem> items = new ArrayList<>();

        // Get deleted folders
        List<Folder> deletedFolders = folderRepository.findAllDeletedByOwnerId(userId);
        for (Folder folder : deletedFolders) {
            // Only show folders deleted directly (not nested in deleted parents)
            if (folder.getParentFolder() == null || !folder.getParentFolder().isDeleted()) {
                String path = buildFolderPath(folder);
                items.add(new SearchDTO.TrashItem(
                        "folder",
                        FolderDTO.FolderResponse.from(folder),
                        path,
                        folder.getDeletedAt().toString()
                ));
            }
        }

        // Get deleted files
        List<FileEntity> deletedFiles = fileRepository.findAllDeletedByOwnerId(userId);
        for (FileEntity file : deletedFiles) {
            // Only show files in non-deleted folders
            if (file.getFolder() == null || !file.getFolder().isDeleted()) {
                String path = buildFilePath(file);
                items.add(new SearchDTO.TrashItem(
                        "file",
                        com.cloudvault.dto.FileDTO.FileResponse.from(file),
                        path,
                        file.getDeletedAt().toString()
                ));
            }
        }

        // Sort by deletion date (most recent first)
        items.sort((a, b) -> b.getDeletedAt().compareTo(a.getDeletedAt()));

        return items;
    }

    @Transactional
    public void restore(UUID userId, String resourceType, UUID resourceId) {
        Share.ResourceType type = Share.ResourceType.valueOf(resourceType.toUpperCase());

        if (type == Share.ResourceType.FOLDER) {
            restoreFolder(userId, resourceId);
        } else {
            restoreFile(userId, resourceId);
        }
    }

    private void restoreFolder(UUID userId, UUID folderId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        // Check permission
        if (permissionService.checkEditPermission(userId, Share.ResourceType.FOLDER, folderId)
                == PermissionService.Permission.NONE) {
            throw new IllegalStateException("Access denied");
        }

        // Restore the folder
        folder.setDeletedAt(null);
        folderRepository.save(folder);

        // If parent was deleted, move to root
        if (folder.getParentFolder() != null && folder.getParentFolder().isDeleted()) {
            folder.setParentFolder(null);
            folderRepository.save(folder);
        }

        // Recursively restore children
        List<Folder> children = folderRepository.findChildrenByParentId(folderId);
        for (Folder child : children) {
            restoreFolder(userId, child.getId());
        }

        // Restore files in this folder
        List<FileEntity> files = fileRepository.findByFolderId(folderId);
        for (FileEntity file : files) {
            if (file.isDeleted()) {
                file.setDeletedAt(null);
                fileRepository.save(file);
            }
        }
    }

    private void restoreFile(UUID userId, UUID fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        // Check permission
        if (permissionService.checkEditPermission(userId, Share.ResourceType.FILE, fileId)
                == PermissionService.Permission.NONE) {
            throw new IllegalStateException("Access denied");
        }

        // Restore the file
        file.setDeletedAt(null);

        // If folder was deleted, move to root
        if (file.getFolder() != null && file.getFolder().isDeleted()) {
            file.setFolder(null);
        }

        fileRepository.save(file);
    }

    @Transactional
    public void permanentDelete(UUID userId, String resourceType, UUID resourceId) {
        Share.ResourceType type = Share.ResourceType.valueOf(resourceType.toUpperCase());

        if (type == Share.ResourceType.FOLDER) {
            permanentDeleteFolder(userId, resourceId);
        } else {
            permanentDeleteFile(userId, resourceId);
        }
    }

    private void permanentDeleteFolder(UUID userId, UUID folderId) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        // Check permission - only owner can permanently delete
        if (!folder.getOwner().getId().equals(userId)) {
            throw new IllegalStateException("Only owner can permanently delete");
        }

        // Delete all children folders recursively
        List<Folder> children = folderRepository.findChildrenByParentId(folderId);
        for (Folder child : children) {
            permanentDeleteFolder(userId, child.getId());
        }

        // Delete all files in this folder
        List<FileEntity> files = fileRepository.findByFolderId(folderId);
        for (FileEntity file : files) {
            // Delete stars
            starRepository.deleteByResource(Share.ResourceType.FILE, file.getId());
            fileRepository.delete(file);
        }

        // Delete stars for this folder
        starRepository.deleteByResource(Share.ResourceType.FOLDER, folderId);

        // Delete the folder
        folderRepository.delete(folder);
    }

    private void permanentDeleteFile(UUID userId, UUID fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        // Check permission - only owner can permanently delete
        if (!file.getOwner().getId().equals(userId)) {
            throw new IllegalStateException("Only owner can permanently delete");
        }

        // Delete stars
        starRepository.deleteByResource(Share.ResourceType.FILE, fileId);

        // Delete the file
        fileRepository.delete(file);
    }

    private String buildFolderPath(Folder folder) {
        List<String> parts = new ArrayList<>();
        Folder current = folder;
        while (current != null) {
            parts.add(0, current.getName());
            current = current.getParentFolder();
        }
        parts.add(0, "My Drive");
        return String.join(" / ", parts);
    }

    private String buildFilePath(FileEntity file) {
        List<String> parts = new ArrayList<>();
        if (file.getFolder() != null) {
            Folder current = file.getFolder();
            while (current != null) {
                parts.add(0, current.getName());
                current = current.getParentFolder();
            }
        }
        parts.add(file.getName());
        parts.add(0, "My Drive");
        return String.join(" / ", parts);
    }
}

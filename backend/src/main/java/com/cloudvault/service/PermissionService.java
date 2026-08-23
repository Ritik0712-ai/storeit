package com.cloudvault.service;

import com.cloudvault.entity.*;
import com.cloudvault.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Permission resolution service implementing the exact logic from 05_Backend_Schema.md §9:
 * 1. Owner → grant access
 * 2. Active share → grant access (with role: viewer/editor)
 * 3. Active public link → grant access (viewer only)
 * 4. Deny
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final ShareRepository shareRepository;
    private final PublicLinkRepository publicLinkRepository;

    public enum Permission {
        NONE, VIEW, EDIT
    }

    /**
     * Check if user has at least the specified permission on a file.
     */
    @Transactional(readOnly = true)
    public Permission checkFilePermission(UUID userId, UUID fileId, Permission required) {
        FileEntity file = fileRepository.findById(fileId).orElse(null);
        if (file == null) {
            return Permission.NONE;
        }

        // 1. Owner check
        if (file.getOwner().getId().equals(userId)) {
            return Permission.EDIT;
        }

        // 2. Share check
        Permission sharePerm = checkSharePermission(userId, Share.ResourceType.FILE, fileId);
        if (sharePerm != Permission.NONE) {
            return sharePerm;
        }

        // 3. Public link check (viewer only)
        Permission linkPerm = checkPublicLinkPermission(Share.ResourceType.FILE, fileId);
        if (linkPerm != Permission.NONE) {
            return linkPerm;
        }

        return Permission.NONE;
    }

    /**
     * Check if user has at least the specified permission on a folder.
     * This includes checking ancestor folders for inherited access.
     */
    @Transactional(readOnly = true)
    public Permission checkFolderPermission(UUID userId, UUID folderId, Permission required) {
        Folder folder = folderRepository.findById(folderId).orElse(null);
        if (folder == null) {
            return Permission.NONE;
        }

        // 1. Owner check (includes all descendants)
        if (folder.getOwner().getId().equals(userId)) {
            return Permission.EDIT;
        }

        // 2. Share check on this folder or any ancestor
        Permission sharePerm = checkAncestorShares(userId, folderId);
        if (sharePerm != Permission.NONE) {
            return sharePerm;
        }

        // 3. Public link check
        Permission linkPerm = checkPublicLinkPermission(Share.ResourceType.FOLDER, folderId);
        if (linkPerm != Permission.NONE) {
            return linkPerm;
        }

        return Permission.NONE;
    }

    /**
     * Check if user can edit (move, delete) a resource.
     * Only owners or editors can edit.
     */
    public Permission checkEditPermission(UUID userId, Share.ResourceType resourceType, UUID resourceId) {
        if (resourceType == Share.ResourceType.FILE) {
            return checkFilePermission(userId, resourceId, Permission.EDIT);
        } else {
            return checkFolderPermission(userId, resourceId, Permission.EDIT);
        }
    }

    /**
     * Check if user can view a resource.
     */
    public Permission checkViewPermission(UUID userId, Share.ResourceType resourceType, UUID resourceId) {
        if (resourceType == Share.ResourceType.FILE) {
            return checkFilePermission(userId, resourceId, Permission.VIEW);
        } else {
            return checkFolderPermission(userId, resourceId, Permission.VIEW);
        }
    }

    private Permission checkSharePermission(UUID userId, Share.ResourceType resourceType, UUID resourceId) {
        return shareRepository.findByResourceAndUser(resourceType, resourceId, userId)
                .map(share -> share.getRole() == Share.Role.EDITOR ? Permission.EDIT : Permission.VIEW)
                .orElse(Permission.NONE);
    }

    private Permission checkAncestorShares(UUID userId, UUID folderId) {
        // First check the folder itself
        Permission directPerm = checkSharePermission(userId, Share.ResourceType.FOLDER, folderId);
        if (directPerm != Permission.NONE) {
            return directPerm;
        }

        // Then check all ancestors
        List<UUID> ancestorIds = folderRepository.findAncestorIds(folderId);
        for (UUID ancestorId : ancestorIds) {
            Permission ancestorPerm = checkSharePermission(userId, Share.ResourceType.FOLDER, ancestorId);
            if (ancestorPerm != Permission.NONE) {
                return ancestorPerm;
            }
        }

        return Permission.NONE;
    }

    private Permission checkPublicLinkPermission(Share.ResourceType resourceType, UUID resourceId) {
        return publicLinkRepository.findActiveByResource(resourceType, resourceId)
                .filter(link -> !link.isExpired() && !link.isRevoked())
                .map(link -> Permission.VIEW)
                .orElse(Permission.NONE);
    }
}

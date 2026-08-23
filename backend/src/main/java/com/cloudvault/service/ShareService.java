package com.cloudvault.service;

import com.cloudvault.dto.ShareDTO;
import com.cloudvault.entity.*;
import com.cloudvault.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareRepository shareRepository;
    private final PublicLinkRepository publicLinkRepository;
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final StarRepository starRepository;
    private final PermissionService permissionService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ShareDTO.ShareResponse createShare(UUID userId, ShareDTO.CreateShareRequest request) {
        Share.ResourceType resourceType = Share.ResourceType.valueOf(request.getResourceType().toUpperCase());
        UUID resourceId = request.getResourceId();
        Share.Role role = Share.Role.valueOf(request.getRole().toUpperCase());

        // Check that user has edit permission on the resource
        if (permissionService.checkEditPermission(userId, resourceType, resourceId)
                == PermissionService.Permission.NONE) {
            throw new IllegalStateException("Access denied to resource");
        }

        // Find target user
        User targetUser = userRepository.findByEmail(request.getTargetUserEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + request.getTargetUserEmail()));

        // Can't share with yourself
        if (targetUser.getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot share with yourself");
        }

        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if share already exists
        Optional<Share> existingShare = shareRepository.findByResourceAndUser(resourceType, resourceId, targetUser.getId());
        if (existingShare.isPresent()) {
            // Update role
            existingShare.get().setRole(role);
            shareRepository.save(existingShare.get());
            return ShareDTO.ShareResponse.from(existingShare.get());
        }

        Share share = Share.builder()
                .resourceType(resourceType)
                .resourceId(resourceId)
                .sharedBy(currentUser)
                .sharedWith(targetUser)
                .role(role)
                .build();

        share = shareRepository.save(share);
        return ShareDTO.ShareResponse.from(share);
    }

    @Transactional(readOnly = true)
    public List<ShareDTO.ShareResponse> getSharedWithMe(UUID userId) {
        List<Share> shares = shareRepository.findAllSharedWithUserId(userId);
        return shares.stream()
                .map(ShareDTO.ShareResponse::from)
                .toList();
    }

    @Transactional
    public void revokeShare(UUID userId, UUID shareId) {
        Share share = shareRepository.findById(shareId)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        // Only the owner can revoke shares
        if (!share.getSharedBy().getId().equals(userId)) {
            throw new IllegalStateException("Access denied");
        }

        shareRepository.delete(share);
    }

    // Public Links

    @Transactional
    public ShareDTO.PublicLinkResponse createPublicLink(UUID userId, ShareDTO.CreatePublicLinkRequest request) {
        Share.ResourceType resourceType = Share.ResourceType.valueOf(request.getResourceType().toUpperCase());
        UUID resourceId = request.getResourceId();

        // Check edit permission on resource
        if (permissionService.checkEditPermission(userId, resourceType, resourceId)
                == PermissionService.Permission.NONE) {
            throw new IllegalStateException("Access denied to resource");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Check if link already exists
        Optional<PublicLink> existingLink = publicLinkRepository.findActiveByResource(resourceType, resourceId);
        if (existingLink.isPresent()) {
            return ShareDTO.PublicLinkResponse.from(existingLink.get(), "");
        }

        String token = UUID.randomUUID().toString() + UUID.randomUUID().toString().replace("-", "");
        String passwordHash = null;
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            passwordHash = passwordEncoder.encode(request.getPassword());
        }

        Instant expiresAt = null;
        if (request.getExpiresAt() != null && !request.getExpiresAt().isBlank()) {
            expiresAt = Instant.parse(request.getExpiresAt());
        }

        PublicLink link = PublicLink.builder()
                .token(token)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .createdBy(user)
                .passwordHash(passwordHash)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        link = publicLinkRepository.save(link);
        return ShareDTO.PublicLinkResponse.from(link, "");
    }

    @Transactional(readOnly = true)
    public ShareDTO.PublicLinkInfoResponse getPublicLinkInfo(String token) {
        PublicLink link = publicLinkRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Link not found"));

        String resourceName;
        String ownerName = link.getCreatedBy().getDisplayName();

        if (link.getResourceType() == Share.ResourceType.FILE) {
            FileEntity file = fileRepository.findById(link.getResourceId()).orElse(null);
            resourceName = file != null ? file.getName() : "Unknown";
        } else {
            Folder folder = folderRepository.findById(link.getResourceId()).orElse(null);
            resourceName = folder != null ? folder.getName() : "Unknown";
        }

        ShareDTO.PublicLinkInfoResponse response = new ShareDTO.PublicLinkInfoResponse();
        response.setResourceType(link.getResourceType().name().toLowerCase());
        response.setResourceName(resourceName);
        response.setOwnerName(ownerName);
        response.setRequiresPassword(link.getPasswordHash() != null);
        response.setExpired(link.isExpired());
        response.setRevoked(link.isRevoked());

        return response;
    }

    @Transactional
    public boolean verifyPublicLinkPassword(String token, String password) {
        PublicLink link = publicLinkRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Link not found"));

        if (link.isExpired() || link.isRevoked()) {
            throw new IllegalStateException("Link is expired or revoked");
        }

        if (link.getPasswordHash() == null) {
            return true;
        }

        return passwordEncoder.matches(password, link.getPasswordHash());
    }

    @Transactional
    public void revokePublicLink(UUID userId, UUID linkId) {
        PublicLink link = publicLinkRepository.findById(linkId)
                .orElseThrow(() -> new IllegalArgumentException("Link not found"));

        if (!link.getCreatedBy().getId().equals(userId)) {
            throw new IllegalStateException("Access denied");
        }

        link.setRevoked(true);
        publicLinkRepository.save(link);
    }

    @Transactional(readOnly = true)
    public String getPublicLinkDownloadUrl(String token) {
        PublicLink link = publicLinkRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Link not found"));

        if (link.isExpired() || link.isRevoked()) {
            throw new IllegalStateException("Link is expired or revoked");
        }

        // TODO: Implement actual download URL generation
        // For now, return a placeholder
        return null;
    }
}

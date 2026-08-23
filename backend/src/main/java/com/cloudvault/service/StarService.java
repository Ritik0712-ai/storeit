package com.cloudvault.service;

import com.cloudvault.dto.FolderDTO;
import com.cloudvault.dto.SearchDTO;
import com.cloudvault.entity.*;
import com.cloudvault.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StarService {

    private final StarRepository starRepository;
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final PermissionService permissionService;

    @Transactional(readOnly = true)
    public List<SearchDTO.StarResponse> getStarred(UUID userId) {
        List<Star> stars = starRepository.findAllByUserId(userId);
        List<SearchDTO.StarResponse> results = new ArrayList<>();

        for (Star star : stars) {
            if (star.getResourceType() == Share.ResourceType.FOLDER) {
                Optional<Folder> folderOpt = folderRepository.findById(star.getResourceId());
                if (folderOpt.isPresent() && !folderOpt.get().isDeleted()) {
                    FolderDTO.FolderResponse folderResp = FolderDTO.FolderResponse.from(folderOpt.get());
                    folderResp.setIsStarred(true);
                    results.add(new SearchDTO.StarResponse("folder", folderResp));
                }
            } else {
                Optional<FileEntity> fileOpt = fileRepository.findById(star.getResourceId());
                if (fileOpt.isPresent() && !fileOpt.get().isDeleted()) {
                    results.add(new SearchDTO.StarResponse("file", com.cloudvault.dto.FileDTO.FileResponse.from(fileOpt.get())));
                }
            }
        }

        return results;
    }

    @Transactional
    public void star(UUID userId, String resourceType, UUID resourceId) {
        Share.ResourceType type = Share.ResourceType.valueOf(resourceType.toUpperCase());

        // Check permission
        if (type == Share.ResourceType.FOLDER) {
            if (permissionService.checkFolderPermission(userId, resourceId, PermissionService.Permission.VIEW)
                    == PermissionService.Permission.NONE) {
                throw new IllegalStateException("Access denied");
            }
        } else {
            if (permissionService.checkFilePermission(userId, resourceId, PermissionService.Permission.VIEW)
                    == PermissionService.Permission.NONE) {
                throw new IllegalStateException("Access denied");
            }
        }

        // Check if already starred
        if (starRepository.existsByUserIdAndResourceTypeAndResourceId(userId, type, resourceId)) {
            return; // Already starred
        }

        User user = new User();
        user.setId(userId);

        Star star = Star.builder()
                .user(user)
                .resourceType(type)
                .resourceId(resourceId)
                .build();

        starRepository.save(star);
    }

    @Transactional
    public void unstar(UUID userId, String resourceType, UUID resourceId) {
        Share.ResourceType type = Share.ResourceType.valueOf(resourceType.toUpperCase());

        Optional<Star> starOpt = starRepository.findByUserAndResource(userId, type, resourceId);
        starOpt.ifPresent(starRepository::delete);
    }

    // Helper to get User entity
    private User getUserById(UUID userId) {
        return folderRepository.findById(userId)
                .map(f -> f.getOwner())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}

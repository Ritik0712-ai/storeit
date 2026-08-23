package com.cloudvault.controller;

import com.cloudvault.dto.FolderDTO;
import com.cloudvault.entity.User;
import com.cloudvault.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @PostMapping
    public ResponseEntity<FolderDTO.FolderResponse> createFolder(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FolderDTO.CreateRequest request) {
        FolderDTO.FolderResponse response = folderService.createFolder(
                user.getId(),
                user,
                request.getName(),
                request.getParentFolderId()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FolderDTO.FolderWithChildrenResponse> getFolder(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        FolderDTO.FolderWithChildrenResponse response = folderService.getFolderWithChildren(
                user.getId(),
                user,
                id
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FolderDTO.FolderResponse> updateFolder(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody FolderDTO.UpdateRequest request) {
        FolderDTO.FolderResponse response = folderService.updateFolder(
                user.getId(),
                user,
                id,
                request.getName(),
                request.getParentFolderId()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        folderService.deleteFolder(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}

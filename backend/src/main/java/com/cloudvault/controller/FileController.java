package com.cloudvault.controller;

import com.cloudvault.dto.FileDTO;
import com.cloudvault.entity.User;
import com.cloudvault.service.FileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/init-upload")
    public ResponseEntity<FileDTO.InitUploadResponse> initUpload(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FileDTO.InitUploadRequest request) {
        FileDTO.InitUploadResponse response = fileService.initUpload(
                user.getId(),
                request
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/complete-upload")
    public ResponseEntity<FileDTO.FileResponse> completeUpload(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        FileDTO.FileResponse response = fileService.completeUpload(
                user.getId(),
                id,
                body != null ? body.get("checksum") : null
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/storage-used")
    public ResponseEntity<Map<String, Long>> getStorageUsed(@AuthenticationPrincipal User user) {
        Long used = fileService.getStorageUsed(user.getId());
        return ResponseEntity.ok(Map.of("storageUsed", used));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FileDTO.FileResponse> getFile(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        FileDTO.FileResponse response = fileService.getFile(user.getId(), id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/download-url")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        String downloadUrl = fileService.getDownloadUrl(user.getId(), id);
        return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<FileDTO.FileResponse> updateFile(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id,
            @RequestBody FileDTO.UpdateRequest request) {
        FileDTO.FileResponse response = fileService.updateFile(
                user.getId(),
                id,
                request.getName(),
                request.getFolderId()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        fileService.deleteFile(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}

package com.cloudvault.controller;

import com.cloudvault.dto.ShareDTO;
import com.cloudvault.entity.User;
import com.cloudvault.service.ShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shares")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;

    @PostMapping
    public ResponseEntity<ShareDTO.ShareResponse> createShare(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ShareDTO.CreateShareRequest request) {
        ShareDTO.ShareResponse response = shareService.createShare(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/shared-with-me")
    public ResponseEntity<java.util.List<ShareDTO.ShareResponse>> getSharedWithMe(
            @AuthenticationPrincipal User user) {
        java.util.List<ShareDTO.ShareResponse> response = shareService.getSharedWithMe(user.getId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeShare(
            @AuthenticationPrincipal User user,
            @PathVariable UUID id) {
        shareService.revokeShare(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}

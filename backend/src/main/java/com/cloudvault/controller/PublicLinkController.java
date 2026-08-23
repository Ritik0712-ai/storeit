package com.cloudvault.controller;

import com.cloudvault.dto.ShareDTO;
import com.cloudvault.entity.User;
import com.cloudvault.service.ShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public-links")
@RequiredArgsConstructor
public class PublicLinkController {

    private final ShareService shareService;

    @PostMapping
    public ResponseEntity<ShareDTO.PublicLinkResponse> createPublicLink(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ShareDTO.CreatePublicLinkRequest request) {
        ShareDTO.PublicLinkResponse response = shareService.createPublicLink(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{token}")
    public ResponseEntity<ShareDTO.PublicLinkInfoResponse> getPublicLinkInfo(
            @PathVariable String token) {
        ShareDTO.PublicLinkInfoResponse response = shareService.getPublicLinkInfo(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{token}/verify")
    public ResponseEntity<Map<String, Boolean>> verifyPassword(
            @PathVariable String token,
            @RequestBody Map<String, String> body) {
        String password = body.get("password");
        boolean valid = shareService.verifyPublicLinkPassword(token, password);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokePublicLink(
            @AuthenticationPrincipal User user,
            @PathVariable java.util.UUID id) {
        shareService.revokePublicLink(user.getId(), id);
        return ResponseEntity.noContent().build();
    }
}

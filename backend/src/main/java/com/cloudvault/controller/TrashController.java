package com.cloudvault.controller;

import com.cloudvault.dto.SearchDTO;
import com.cloudvault.entity.User;
import com.cloudvault.service.TrashService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/trash")
@RequiredArgsConstructor
public class TrashController {

    private final TrashService trashService;

    @GetMapping
    public ResponseEntity<List<SearchDTO.TrashItem>> getTrash(
            @AuthenticationPrincipal User user) {
        List<SearchDTO.TrashItem> items = trashService.getTrash(user.getId());
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{resourceType}/{resourceId}/restore")
    public ResponseEntity<Void> restore(
            @AuthenticationPrincipal User user,
            @PathVariable String resourceType,
            @PathVariable UUID resourceId) {
        trashService.restore(user.getId(), resourceType, resourceId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{resourceType}/{resourceId}")
    public ResponseEntity<Void> permanentDelete(
            @AuthenticationPrincipal User user,
            @PathVariable String resourceType,
            @PathVariable UUID resourceId) {
        trashService.permanentDelete(user.getId(), resourceType, resourceId);
        return ResponseEntity.noContent().build();
    }
}

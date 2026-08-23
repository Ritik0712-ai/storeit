package com.cloudvault.controller;

import com.cloudvault.dto.SearchDTO;
import com.cloudvault.entity.User;
import com.cloudvault.service.StarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stars")
@RequiredArgsConstructor
public class StarController {

    private final StarService starService;

    @GetMapping
    public ResponseEntity<List<SearchDTO.StarResponse>> getStarred(
            @AuthenticationPrincipal User user) {
        List<SearchDTO.StarResponse> results = starService.getStarred(user.getId());
        return ResponseEntity.ok(results);
    }

    @PostMapping("/{resourceType}/{resourceId}")
    public ResponseEntity<Void> star(
            @AuthenticationPrincipal User user,
            @PathVariable String resourceType,
            @PathVariable UUID resourceId) {
        starService.star(user.getId(), resourceType, resourceId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{resourceType}/{resourceId}")
    public ResponseEntity<Void> unstar(
            @AuthenticationPrincipal User user,
            @PathVariable String resourceType,
            @PathVariable UUID resourceId) {
        starService.unstar(user.getId(), resourceType, resourceId);
        return ResponseEntity.noContent().build();
    }
}

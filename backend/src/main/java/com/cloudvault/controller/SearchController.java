package com.cloudvault.controller;

import com.cloudvault.dto.SearchDTO;
import com.cloudvault.entity.User;
import com.cloudvault.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<List<SearchDTO.SearchResult>> search(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "all") String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        List<SearchDTO.SearchResult> results = searchService.search(user.getId(), q, type);
        return ResponseEntity.ok(results);
    }
}

package com.cloudvault.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class SearchDTO {

    @Data
    public static class SearchResult {
        private String type;
        private Object item;
        private String path;

        public SearchResult(String type, Object item, String path) {
            this.type = type;
            this.item = item;
            this.path = path;
        }
    }

    @Data
    public static class StarResponse {
        private String type;
        private Object item;

        public StarResponse(String type, Object item) {
            this.type = type;
            this.item = item;
        }
    }

    @Data
    public static class TrashItem {
        private String type;
        private Object item;
        private String originalPath;
        private String deletedAt;

        public TrashItem(String type, Object item, String originalPath, String deletedAt) {
            this.type = type;
            this.item = item;
            this.originalPath = originalPath;
            this.deletedAt = deletedAt;
        }
    }
}

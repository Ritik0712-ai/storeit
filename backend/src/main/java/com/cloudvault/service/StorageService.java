package com.cloudvault.service;

import com.cloudvault.config.SupabaseStorageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final SupabaseStorageConfig storageConfig;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Generate a signed upload URL for direct upload to Supabase Storage.
     */
    public String generateUploadUrl(UUID fileId) {
        String storagePath = generateStoragePath(fileId);

        // Generate a signed upload URL via Supabase REST API
        String url = String.format("%s/storage/v1/object/upload/sign/%s/%s",
                storageConfig.getUrl(),
                storageConfig.getStorageBucket(),
                storagePath);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + storageConfig.getServiceRoleKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse the response to get the signed URL fragment
                // Response is JSON like: {"url": "/object/upload/sign/cloudvault-files/...?token=..."}
                String responseBody = response.body();
                int urlStart = responseBody.indexOf("\"url\":\"") + 7;
                int urlEnd = responseBody.indexOf("\"", urlStart);
                String urlFragment = responseBody.substring(urlStart, urlEnd);
                
                return storageConfig.getUrl() + "/storage/v1" + urlFragment;
            }

            throw new RuntimeException("Failed to generate upload URL: " + response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to generate upload URL", e);
        }
    }

    /**
     * Generate a signed download URL for accessing a file.
     */
    public String generateDownloadUrl(UUID fileId, String storagePath) {
        // Generate a temporary download URL (valid for 1 hour)
        // Using Supabase Storage's REST API
        String url = String.format("%s/storage/v1/object/sign/%s/%s",
                storageConfig.getUrl(),
                storageConfig.getStorageBucket(),
                storagePath);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + storageConfig.getServiceRoleKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"expiresIn\": 3600}"))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parse the response to get the signed URL
                // The response is JSON: {"signedURL": "/object/sign/..."}
                String responseBody = response.body();
                int urlStart = responseBody.indexOf("\"signedURL\":\"") + 13;
                int urlEnd = responseBody.indexOf("\"", urlStart);
                String urlFragment = responseBody.substring(urlStart, urlEnd);
                return storageConfig.getUrl() + "/storage/v1" + urlFragment;
            }

            throw new RuntimeException("Failed to generate download URL: " + response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to generate download URL", e);
        }
    }

    /**
     * Delete a file from Supabase Storage.
     */
    public void deleteFile(String storagePath) {
        String url = String.format("%s/storage/v1/object/%s/%s",
                storageConfig.getUrl(),
                storageConfig.getStorageBucket(),
                storagePath);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + storageConfig.getServiceRoleKey())
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 204) {
                throw new RuntimeException("Failed to delete file: " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    public String generateStoragePath(UUID fileId) {
        return String.format("uploads/%s", fileId.toString());
    }
}

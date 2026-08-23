package com.cloudvault.service;

import com.cloudvault.config.SupabaseStorageConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfilePictureService {

    private final SupabaseStorageConfig storageConfig;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static final String PROFILE_BUCKET = "cloudvault-profiles";

    /**
     * Uploads a profile picture to Supabase and returns the public URL.
     */
    public String uploadProfilePicture(UUID userId, MultipartFile file) {
        String fileName = userId.toString() + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
        String url = String.format("%s/storage/v1/object/%s/%s",
                storageConfig.getUrl(),
                PROFILE_BUCKET,
                fileName);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + storageConfig.getServiceRoleKey())
                    .header("Content-Type", file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return String.format("%s/storage/v1/object/public/%s/%s",
                        storageConfig.getUrl(),
                        PROFILE_BUCKET,
                        fileName);
            }

            throw new RuntimeException("Failed to upload profile picture: " + response.body());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to upload profile picture", e);
        }
    }
}

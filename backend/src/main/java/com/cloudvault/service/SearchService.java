package com.cloudvault.service;

import com.cloudvault.dto.FolderDTO;
import com.cloudvault.dto.SearchDTO;
import com.cloudvault.entity.*;
import com.cloudvault.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final StarRepository starRepository;
    private final ShareRepository shareRepository;

    @Transactional(readOnly = true)
    public List<SearchDTO.SearchResult> search(UUID userId, String query, String type) {
        List<SearchDTO.SearchResult> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        boolean searchFiles = type == null || type.equals("all") || type.equals("file");
        boolean searchFolders = type == null || type.equals("all") || type.equals("folder");

        // Search folders
        if (searchFolders) {
            List<Folder> userFolders = folderRepository.findAllActiveByOwnerId(userId);
            for (Folder folder : userFolders) {
                if (folder.getName().toLowerCase().contains(lowerQuery)) {
                    String path = buildFolderPath(folder);
                    SearchDTO.SearchResult result = new SearchDTO.SearchResult(
                            "folder",
                            FolderDTO.FolderResponse.from(folder),
                            path
                    );
                    results.add(result);
                }
            }

            // Also search shared folders
            List<Share> sharedFolders = shareRepository.findAllSharedWithUserId(userId);
            for (Share share : sharedFolders) {
                if (share.getResourceType() == Share.ResourceType.FOLDER) {
                    Optional<Folder> folderOpt = folderRepository.findById(share.getResourceId());
                    if (folderOpt.isPresent() && !folderOpt.get().isDeleted()) {
                        Folder folder = folderOpt.get();
                        if (folder.getName().toLowerCase().contains(lowerQuery)) {
                            String path = buildFolderPath(folder);
                            SearchDTO.SearchResult result = new SearchDTO.SearchResult(
                                    "folder",
                                    FolderDTO.FolderResponse.from(folder),
                                    path
                            );
                            results.add(result);
                        }
                    }
                }
            }
        }

        // Search files
        if (searchFiles) {
            List<FileEntity> userFiles = fileRepository.findAllActiveByOwnerId(userId);
            for (FileEntity file : userFiles) {
                if (file.getName().toLowerCase().contains(lowerQuery)) {
                    String path = buildFilePath(file);
                    SearchDTO.SearchResult result = new SearchDTO.SearchResult(
                            "file",
                            com.cloudvault.dto.FileDTO.FileResponse.from(file),
                            path
                    );
                    results.add(result);
                }
            }

            // Also search shared files
            List<Share> sharedFiles = shareRepository.findAllSharedWithUserId(userId);
            for (Share share : sharedFiles) {
                if (share.getResourceType() == Share.ResourceType.FILE) {
                    Optional<FileEntity> fileOpt = fileRepository.findById(share.getResourceId());
                    if (fileOpt.isPresent() && !fileOpt.get().isDeleted()) {
                        FileEntity file = fileOpt.get();
                        if (file.getName().toLowerCase().contains(lowerQuery)) {
                            String path = buildFilePath(file);
                            SearchDTO.SearchResult result = new SearchDTO.SearchResult(
                                    "file",
                                    com.cloudvault.dto.FileDTO.FileResponse.from(file),
                                    path
                            );
                            results.add(result);
                        }
                    }
                }
            }
        }

        return results;
    }

    private String buildFolderPath(Folder folder) {
        List<String> parts = new ArrayList<>();
        Folder current = folder;
        while (current != null) {
            parts.add(0, current.getName());
            current = current.getParentFolder();
        }
        parts.add(0, "My Drive");
        return String.join(" / ", parts);
    }

    private String buildFilePath(FileEntity file) {
        List<String> parts = new ArrayList<>();
        if (file.getFolder() != null) {
            Folder current = file.getFolder();
            while (current != null) {
                parts.add(0, current.getName());
                current = current.getParentFolder();
            }
        }
        parts.add(file.getName());
        parts.add(0, "My Drive");
        return String.join(" / ", parts);
    }
}

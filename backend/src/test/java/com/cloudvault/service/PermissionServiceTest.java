package com.cloudvault.service;

import com.cloudvault.entity.*;
import com.cloudvault.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private FolderRepository folderRepository;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private ShareRepository shareRepository;

    @Mock
    private PublicLinkRepository publicLinkRepository;

    @InjectMocks
    private PermissionService permissionService;

    private UUID userId;
    private UUID ownerId;
    private User owner;
    private User otherUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ownerId = UUID.randomUUID();

        owner = User.builder()
                .id(ownerId)
                .email("owner@example.com")
                .displayName("Owner")
                .authProvider(User.AuthProvider.local)
                .build();

        otherUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .displayName("User")
                .authProvider(User.AuthProvider.local)
                .build();
    }

    @Test
    void shouldGrantEditPermissionToOwner() {
        UUID fileId = UUID.randomUUID();
        FileEntity file = FileEntity.builder()
                .id(fileId)
                .name("test.txt")
                .owner(owner)
                .build();

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));

        PermissionService.Permission result =
                permissionService.checkFilePermission(ownerId, fileId, PermissionService.Permission.VIEW);

        assertEquals(PermissionService.Permission.EDIT, result);
    }

    @Test
    void shouldGrantViewerPermissionFromShare() {
        UUID fileId = UUID.randomUUID();
        FileEntity file = FileEntity.builder()
                .id(fileId)
                .name("test.txt")
                .owner(owner)
                .build();

        Share share = Share.builder()
                .resourceType(Share.ResourceType.FILE)
                .resourceId(fileId)
                .sharedWith(otherUser)
                .role(Share.Role.VIEWER)
                .build();

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(shareRepository.findByResourceAndUser(Share.ResourceType.FILE, fileId, userId))
                .thenReturn(Optional.of(share));

        PermissionService.Permission result =
                permissionService.checkFilePermission(userId, fileId, PermissionService.Permission.VIEW);

        assertEquals(PermissionService.Permission.VIEW, result);
    }

    @Test
    void shouldDenyPermissionWhenNoAccess() {
        UUID fileId = UUID.randomUUID();
        FileEntity file = FileEntity.builder()
                .id(fileId)
                .name("test.txt")
                .owner(owner)
                .build();

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(shareRepository.findByResourceAndUser(any(), any(), any())).thenReturn(Optional.empty());
        when(publicLinkRepository.findActiveByResource(any(), any())).thenReturn(Optional.empty());

        PermissionService.Permission result =
                permissionService.checkFilePermission(userId, fileId, PermissionService.Permission.VIEW);

        assertEquals(PermissionService.Permission.NONE, result);
    }

    @Test
    void shouldReturnNoneForNonExistentFile() {
        UUID fileId = UUID.randomUUID();

        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        PermissionService.Permission result =
                permissionService.checkFilePermission(userId, fileId, PermissionService.Permission.VIEW);

        assertEquals(PermissionService.Permission.NONE, result);
    }
}

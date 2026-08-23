package com.cloudvault.repository;

import com.cloudvault.entity.PublicLink;
import com.cloudvault.entity.Share;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PublicLinkRepository extends JpaRepository<PublicLink, UUID> {

    Optional<PublicLink> findByToken(String token);

    @Query("SELECT p FROM PublicLink p WHERE p.resourceType = :resourceType AND p.resourceId = :resourceId AND p.revoked = false")
    Optional<PublicLink> findActiveByResource(@Param("resourceType") Share.ResourceType resourceType,
                                               @Param("resourceId") UUID resourceId);

    @Query("SELECT p FROM PublicLink p WHERE p.createdBy.id = :userId")
    List<PublicLink> findAllByCreatorId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE PublicLink p SET p.revoked = true WHERE p.resourceType = :resourceType AND p.resourceId = :resourceId")
    void revokeByResource(@Param("resourceType") Share.ResourceType resourceType,
                          @Param("resourceId") UUID resourceId);
}

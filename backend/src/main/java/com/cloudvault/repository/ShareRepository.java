package com.cloudvault.repository;

import com.cloudvault.entity.Share;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShareRepository extends JpaRepository<Share, UUID> {

    @Query("SELECT s FROM Share s WHERE s.sharedWith.id = :userId")
    List<Share> findAllSharedWithUserId(@Param("userId") UUID userId);

    @Query("SELECT s FROM Share s WHERE s.resourceType = :resourceType AND s.resourceId = :resourceId")
    List<Share> findByResource(@Param("resourceType") Share.ResourceType resourceType,
                               @Param("resourceId") UUID resourceId);

    @Query("SELECT s FROM Share s WHERE s.resourceType = :resourceType AND s.resourceId = :resourceId AND s.sharedWith.id = :userId")
    Optional<Share> findByResourceAndUser(@Param("resourceType") Share.ResourceType resourceType,
                                          @Param("resourceId") UUID resourceId,
                                          @Param("userId") UUID userId);

    @Query("SELECT s FROM Share s WHERE s.sharedBy.id = :userId")
    List<Share> findAllSharedByUserId(@Param("userId") UUID userId);

    void deleteByResourceTypeAndResourceId(Share.ResourceType resourceType, UUID resourceId);
}

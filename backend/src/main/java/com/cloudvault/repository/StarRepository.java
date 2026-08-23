package com.cloudvault.repository;

import com.cloudvault.entity.Share;
import com.cloudvault.entity.Star;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StarRepository extends JpaRepository<Star, UUID> {

    @Query("SELECT s FROM Star s WHERE s.user.id = :userId")
    List<Star> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT s FROM Star s WHERE s.user.id = :userId AND s.resourceType = :resourceType AND s.resourceId = :resourceId")
    Optional<Star> findByUserAndResource(@Param("userId") UUID userId,
                                          @Param("resourceType") Share.ResourceType resourceType,
                                          @Param("resourceId") UUID resourceId);

    @Modifying
    @Query("DELETE FROM Star s WHERE s.resourceType = :resourceType AND s.resourceId = :resourceId")
    void deleteByResource(@Param("resourceType") Share.ResourceType resourceType,
                           @Param("resourceId") UUID resourceId);

    boolean existsByUserIdAndResourceTypeAndResourceId(UUID userId, Share.ResourceType resourceType, UUID resourceId);
}

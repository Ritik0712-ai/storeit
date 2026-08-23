package com.cloudvault.repository;

import com.cloudvault.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FileRepository extends JpaRepository<FileEntity, UUID> {

    @Query("SELECT f FROM FileEntity f WHERE f.folder IS NULL AND f.owner.id = :ownerId AND f.deletedAt IS NULL")
    List<FileEntity> findRootFilesByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT f FROM FileEntity f WHERE f.folder.id = :folderId AND f.deletedAt IS NULL")
    List<FileEntity> findByFolderId(@Param("folderId") UUID folderId);

    @Query("SELECT f FROM FileEntity f WHERE f.owner.id = :ownerId AND f.deletedAt IS NULL")
    List<FileEntity> findAllActiveByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT f FROM FileEntity f WHERE f.owner.id = :ownerId AND f.deletedAt IS NOT NULL")
    List<FileEntity> findAllDeletedByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT f FROM FileEntity f LEFT JOIN FETCH f.owner WHERE f.id = :id AND f.deletedAt IS NULL")
    Optional<FileEntity> findByIdWithOwner(@Param("id") UUID id);

    @Query("SELECT f FROM FileEntity f WHERE f.owner.id = :ownerId AND LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%')) AND f.deletedAt IS NULL")
    List<FileEntity> searchByName(@Param("ownerId") UUID ownerId, @Param("query") String query);
}

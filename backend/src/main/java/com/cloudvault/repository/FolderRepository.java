package com.cloudvault.repository;

import com.cloudvault.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FolderRepository extends JpaRepository<Folder, UUID> {

    @Query("SELECT f FROM Folder f WHERE f.parentFolder IS NULL AND f.owner.id = :ownerId AND f.deletedAt IS NULL")
    List<Folder> findRootFoldersByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT f FROM Folder f WHERE f.parentFolder.id = :parentId AND f.deletedAt IS NULL")
    List<Folder> findChildrenByParentId(@Param("parentId") UUID parentId);

    @Query("SELECT f FROM Folder f WHERE f.owner.id = :ownerId AND f.deletedAt IS NULL")
    List<Folder> findAllActiveByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT f FROM Folder f WHERE f.owner.id = :ownerId AND f.deletedAt IS NOT NULL")
    List<Folder> findAllDeletedByOwnerId(@Param("ownerId") UUID ownerId);

    @Query("SELECT f FROM Folder f LEFT JOIN FETCH f.owner WHERE f.id = :id AND f.deletedAt IS NULL")
    Optional<Folder> findByIdWithOwner(@Param("id") UUID id);

    @Query(value = "WITH RECURSIVE ancestors AS (" +
            "  SELECT id, parent_folder_id FROM folders WHERE id = :folderId " +
            "  UNION ALL " +
            "  SELECT f.id, f.parent_folder_id FROM folders f INNER JOIN ancestors a ON f.id = a.parent_folder_id" +
            ") SELECT id FROM ancestors WHERE id != :folderId", nativeQuery = true)
    List<UUID> findAncestorIds(@Param("folderId") UUID folderId);
}

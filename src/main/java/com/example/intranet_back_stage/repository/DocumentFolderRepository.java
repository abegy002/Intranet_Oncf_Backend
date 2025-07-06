// DocumentFolderRepository.java
package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.model.DocumentFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentFolderRepository extends JpaRepository<DocumentFolder, Long> {

    List<DocumentFolder> findByDocumentSpaceIdAndParentFolderIsNull(Long documentSpaceId);

    List<DocumentFolder> findByParentFolderId(Long parentFolderId);

    List<DocumentFolder> findByDocumentSpaceId(Long documentSpaceId);

    @Query("SELECT f FROM DocumentFolder f WHERE f.documentSpace.id = :spaceId AND f.parentFolder IS NULL")
    List<DocumentFolder> findRootFoldersBySpaceId(@Param("spaceId") Long spaceId);

    @Query("SELECT f FROM DocumentFolder f WHERE f.parentFolder.id = :parentId")
    List<DocumentFolder> findSubFoldersByParentId(@Param("parentId") Long parentId);

    boolean existsByNameAndDocumentSpaceIdAndParentFolderId(String name, Long documentSpaceId, Long parentFolderId);

    boolean existsByNameAndDocumentSpaceIdAndParentFolderIsNull(String name, Long documentSpaceId);
}
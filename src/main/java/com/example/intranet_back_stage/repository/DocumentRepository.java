// DocumentRepository.java
package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.model.Document;
import com.example.intranet_back_stage.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByDocumentSpaceIdAndFolderIsNull(Long documentSpaceId);

    List<Document> findByFolderId(Long folderId);

    List<Document> findByDocumentSpaceId(Long documentSpaceId);

    List<Document> findByUploadedById(Long uploadedById);

    List<Document> findByDocumentType(DocumentType documentType);

    @Query("SELECT d FROM Document d WHERE d.documentSpace.id = :spaceId AND d.folder IS NULL")
    List<Document> findRootDocumentsBySpaceId(@Param("spaceId") Long spaceId);

    @Query("SELECT d FROM Document d WHERE d.name LIKE %:name% OR d.originalName LIKE %:name%")
    List<Document> findByNameContaining(@Param("name") String name);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.folder.id = :folderId")
    int countDocumentsByFolderId(@Param("folderId") Long folderId);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.documentSpace.id = :spaceId AND d.folder IS NULL")
    int countRootDocumentsBySpaceId(@Param("spaceId") Long spaceId);
}
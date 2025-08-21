package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.model.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DocumentRepository
        extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    Page<Document> findByTitleContainingIgnoreCaseOrDocTypeContainingIgnoreCase(
            String q1, String q2, Pageable pageable);

    // optional helpers if you don’t want Specification (we still keep Spec in service)
    Page<Document> findByFolderId(Long folderId, Pageable pageable);
    Page<Document> findByFolderIsNull(Pageable pageable);

    long countByFolderId(Long folderId);
}

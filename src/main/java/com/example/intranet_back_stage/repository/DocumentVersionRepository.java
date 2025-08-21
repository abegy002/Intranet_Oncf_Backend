package com.example.intranet_back_stage.repository;

import java.util.List;

import com.example.intranet_back_stage.model.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    List<DocumentVersion> findByDocumentIdOrderByCreatedAtDesc(Long docId);
}

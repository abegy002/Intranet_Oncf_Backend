package com.example.intranet_back_stage.repository;

// DocumentAuditRepository.java
import java.util.List;

import com.example.intranet_back_stage.model.DocumentAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentAuditRepository extends JpaRepository<DocumentAudit, Long> {
    List<DocumentAudit> findByDocumentIdOrderByAtDesc(Long docId);
}


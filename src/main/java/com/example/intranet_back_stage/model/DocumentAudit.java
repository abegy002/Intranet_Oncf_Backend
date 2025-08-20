package com.example.intranet_back_stage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// docs/model/DocumentAudit.java
@Entity
@Table(name="document_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAudit {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY) private Long id;

    private Long documentId;
    private String action;          // VIEW, DOWNLOAD, CREATE, UPDATE, APPROVE
    private String actor;
    private LocalDateTime at = LocalDateTime.now();
    @Column(length=1000) private String details;
}


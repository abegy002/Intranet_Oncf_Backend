// src/main/java/.../model/Document.java
package com.example.intranet_back_stage.model;

import com.example.intranet_back_stage.enums.DocumentStatus;
import com.example.intranet_back_stage.enums.DocumentType;
import com.example.intranet_back_stage.enums.Sensitivity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity @Table(name="documents")
@Data @NoArgsConstructor @AllArgsConstructor
public class Document {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="folder_id")
    private Folder folder;

    @Column(nullable=false) private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length = 40)
    private DocumentType docType;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length = 40)
    private DocumentStatus status; // EN_REVUE on create

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length = 40)
    private Sensitivity sensitivity; // default INTERNE

    private String owner; // username

    // --- review / lifecycle info ---
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String rejectionReason;

    private LocalDateTime publishedAt;
    private LocalDateTime abrogatedAt;

    @Column(nullable=false) private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
}

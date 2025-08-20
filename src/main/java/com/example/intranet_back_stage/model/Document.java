package com.example.intranet_back_stage.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// docs/model/Document.java
@Entity @Table(name="documents")
@Data @NoArgsConstructor @AllArgsConstructor
public class Document {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="folder_id")
    private Folder folder;

    @Column(nullable=false) private String title;
    @Column(nullable=false) private String docType;   // ex: "ATTESTATION","JUSTIFICATIF","POLITIQUE"
    @Column(nullable=false) private String status;    // "BROUILLON","EN_REVUE","PUBLIE","ABROGE"
    private String sensitivity;                      // "INTERNE","CONFIDENTIEL"...
    private String owner;                            // username

    @Column(nullable=false) private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
}

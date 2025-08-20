// DocumentSpace.java
package com.example.intranet_back_stage.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity @Table(name="document_versions")
@Data @NoArgsConstructor @AllArgsConstructor
public class DocumentVersion {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="document_id", nullable=false)
    private Document document;

    @Column(nullable=false) private String versionNo;   // "1.0", "1.1", "2.0"
    @Column(nullable=false) private String filename;
    @Column(nullable=false) private String storageKey;  // chemin S3/MinIO ou disque
    private Long size;
    private String checksum;                            // SHA-256

    @Column(nullable=false) private String createdBy;   // username
    @Column(nullable=false) private LocalDateTime createdAt = LocalDateTime.now();
    private String comment;
}

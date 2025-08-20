// model/Folder.java
package com.example.intranet_back_stage.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity @Table(name = "folders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Folder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=150)
    private String name;

    /** unique, path-like e.g. "/Policies/HR" */
    @Column(nullable=false, unique=true, length=512)
    private String path;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="parent_id")
    private Folder parent;

    private LocalDateTime createdAt = LocalDateTime.now();
}

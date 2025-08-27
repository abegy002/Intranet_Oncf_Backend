// src/main/java/com/example/intranet_back_stage/model/Event.java
package com.example.intranet_back_stage.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "events")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Event {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false) private String title;
    @Column(length=4000) private String description;
    private String location;

    @Column(nullable=false) private LocalDateTime startsAt;
    private LocalDateTime endsAt;

    @Column(nullable=false) private String status = "PLANNED";     // PLANNED|CANCELLED|POSTPONED
    @Column(nullable=false) private String visibility = "INTERNAL"; // INTERNAL|PUBLIC
    private Integer capacity;

    // Optional cover image stored via your StorageService (MinIO)
    private String coverKey;

    private String createdBy;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt;
}

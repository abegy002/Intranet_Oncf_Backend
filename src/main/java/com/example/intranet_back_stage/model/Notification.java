package com.example.intranet_back_stage.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_recipient_created", columnList = "recipientId, createdAt DESC"),
        @Index(name = "idx_notif_recipient_unread", columnList = "recipientId, readAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Target user (recipient) */
    @Column(nullable = false)
    private Long recipientId;

    /** Optional username cache for convenience (fast fanout to users by username) */
    @Column(length = 120)
    private String recipientUsername;

    /** Optional: who triggered it (admin/HR/employee username) */
    @Column(length = 120)
    private String actorUsername;

    /** Type codes: ATTESTATION_REQUEST, LEAVE_REQUEST, REQUEST_DECISION, DOCUMENT_ADDED, GENERIC... */
    @Column(length = 60, nullable = false)
    private String type;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(length = 1000)
    private String message;

    /** Optional JSON payload for future extension */
    @Lob
    private String payloadJson;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** When user opened/clicked/acknowledged */
    private LocalDateTime readAt;
}

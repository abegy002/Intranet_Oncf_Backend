package com.example.intranet_back_stage.model;


import com.example.intranet_back_stage.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "attestation_requests")
public class AttestationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "employee_id")
    private User employee;

    @Enumerated(EnumType.STRING)
    private AttestationType attestationType;

    @Enumerated(EnumType.STRING)
    private AttestationStatus status = AttestationStatus.EN_ATTENTE;

    private String signedDocumentPath;
    private String signedDocumentFilename;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime processedAt;
    private LocalDateTime sentAt;
    private String processedBy;

    public enum AttestationType { TRAVAIL, SALAIRE;
        public static AttestationType fromString(String value) {
            return AttestationType.valueOf(value.toUpperCase());
        }
    }
    public enum AttestationStatus { EN_ATTENTE, EN_COURS, SIGNE, ENVOYE, REJETE }
}
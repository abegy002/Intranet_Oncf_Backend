package com.example.intranet_back_stage.model;

import com.example.intranet_back_stage.enums.Status;
import com.example.intranet_back_stage.model.User;
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

    // L’utilisateur qui fait la demande
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Statut : PENDING, APPROVED, REJECTED
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    // Type d'attestation demandée (ex : "Travail", "Salaire", ...)
    private String attestationType;

    // Date de la demande
    private LocalDateTime requestDate = LocalDateTime.now();

    // Le fichier attestation envoyé par le RH (en bytes)
    @Column(columnDefinition = "BYTEA")
    private byte[] attestationFile;

    private String fileName; // ex: "attestation_2025.pdf"
    private String fileType; // ex: "application/pdf"
}


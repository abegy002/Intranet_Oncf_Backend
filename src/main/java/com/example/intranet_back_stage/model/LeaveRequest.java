package com.example.intranet_back_stage.model;

import com.example.intranet_back_stage.enums.Status;
import com.example.intranet_back_stage.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;




// model/LeaveRequest.java
@Entity @Table(name = "leave_requests")
@Data @NoArgsConstructor @AllArgsConstructor
public class LeaveRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private LeaveType type; // ANNUEL, EXCEPTIONNEL, MATERNITE, PATERNITE, MALADIE, ACCIDENT, HOSPITALISATION

    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private LeaveStatus status; // EN_ATTENTE, APPROUVE, REJETE, ANNULE

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(length = 500)
    private String reason; // optionnel: motif employé

    private String justificatifPath;    // pour non planifiable (pdf/jpeg…)
    private String justificatifFilename;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime decidedAt;
    private String decidedBy; // username RH

    // jours ouvrés calculés et figés à l'approbation (sécurité paie / historique)
    @Column(precision = 5, scale = 2)
    private BigDecimal approvedDays;

    public enum LeaveType { ANNUEL, EXCEPTIONNEL, MATERNITE, PATERNITE, MALADIE, ACCIDENT, HOSPITALISATION }
    public enum LeaveStatus { EN_ATTENTE, APPROUVE, REJETE, ANNULE }
}

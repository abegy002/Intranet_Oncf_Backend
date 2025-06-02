package com.example.intranet_back_stage.model;

import com.example.intranet_back_stage.enums.Status;
import com.example.intranet_back_stage.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // ex: "annuel", "maladie", "exceptionnel"

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING; // "EN_ATTENTE", "APPROUVÉ", "REFUSÉ"

    private String reason; // justification du congé

    private LocalDate startDate;
    private LocalDate endDate;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDateTime requestDate;
}


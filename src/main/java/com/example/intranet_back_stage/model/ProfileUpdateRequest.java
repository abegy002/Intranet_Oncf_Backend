package com.example.intranet_back_stage.model;

import com.example.intranet_back_stage.enums.Status;
import com.example.intranet_back_stage.model.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    private String firstname;
    private String lastname;
    private String email;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    private String rejectionReason;
}

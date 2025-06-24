package com.example.intranet_back_stage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaveRequestDTO {
    private Long id;
    private String type;
    private String status;
    private String reason;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime requestDate;
    private Long userId;
    private String userName;  // par exemple user.firstname + user.lastname
}


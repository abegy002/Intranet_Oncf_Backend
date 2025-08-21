package com.example.intranet_back_stage.dto;

import com.example.intranet_back_stage.model.LeaveRequest;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// dto/LeaveRequestCreateDTO.java (employé)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaveRequestCreateDTO {
    @NotNull private Long userId;
    @NotNull private LeaveRequest.LeaveType type;     // doit être planifiable
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    private String reason;
}


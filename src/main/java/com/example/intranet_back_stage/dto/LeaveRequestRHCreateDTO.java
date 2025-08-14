package com.example.intranet_back_stage.dto;

import com.example.intranet_back_stage.model.LeaveRequest;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaveRequestRHCreateDTO {
    @NotBlank
    private String employeeCode;

    @NotNull
    private LeaveRequest.LeaveType type;   // non planifiable

    // If you’re not sure jsr310 module is present, keep @JsonFormat
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String reason;
}

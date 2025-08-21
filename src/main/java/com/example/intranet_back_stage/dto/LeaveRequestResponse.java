// com/example/intranet_back_stage/dto/LeaveRequestResponse.java
package com.example.intranet_back_stage.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveRequestResponse(
        Long id,
        String type,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        String justificatifFilename,
        LocalDateTime createdAt,
        LocalDateTime decidedAt,
        String decidedBy,
        String employeeName
) {}

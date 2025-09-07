// src/main/java/.../dto/DocumentDto.java
package com.example.intranet_back_stage.dto;


import com.example.intranet_back_stage.enums.DocumentStatus;
import com.example.intranet_back_stage.enums.DocumentType;
import com.example.intranet_back_stage.enums.Sensitivity;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentDto(
        Long id,
        String title,
        DocumentType docType,
        DocumentStatus status,
        Sensitivity sensitivity,
        String owner,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<VersionDto> versions,
        String iconUrl,
        // review info
        String reviewedBy,
        LocalDateTime reviewedAt,
        String rejectionReason,
        LocalDateTime publishedAt,
        LocalDateTime abrogatedAt
) {}

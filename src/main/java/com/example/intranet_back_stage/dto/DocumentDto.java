package com.example.intranet_back_stage.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DocumentDto(
        Long id,
        String title,
        String docType,
        String status,
        String sensitivity,
        String owner,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<VersionDto> versions,
        String iconUrl // <-- NEW
){}

package com.example.intranet_back_stage.dto;

import java.time.LocalDateTime;

public record VersionDto(
        Long id,
        String versionNo,
        String filename,
        Long size,
        String createdBy,
        LocalDateTime createdAt
){}
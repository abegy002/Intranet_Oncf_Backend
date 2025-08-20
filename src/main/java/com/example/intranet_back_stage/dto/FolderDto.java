// dto/FolderDto.java
package com.example.intranet_back_stage.dto;

import java.time.LocalDateTime;

public record FolderDto(
        Long id,
        String name,
        String path,
        Long parentId,
        LocalDateTime createdAt,
        int childrenCount
) {}

// dto/FolderNodeDto.java
package com.example.intranet_back_stage.dto;

import java.util.List;

public record FolderNodeDto(
        Long id,
        String name,
        String path,
        List<FolderNodeDto> children
) {}

// src/main/java/.../dto/DocumentCreateDto.java
package com.example.intranet_back_stage.dto;


import com.example.intranet_back_stage.enums.DocumentType;
import com.example.intranet_back_stage.enums.Sensitivity;

public record DocumentCreateDto(
        Long folderId,
        String title,
        DocumentType docType,
        Sensitivity sensitivity
) {}

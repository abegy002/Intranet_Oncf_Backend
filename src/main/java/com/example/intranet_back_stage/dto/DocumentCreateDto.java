package com.example.intranet_back_stage.dto;

public record DocumentCreateDto(
        Long folderId,
        String title,
        String docType,
        String status,
        String sensitivity
) {}


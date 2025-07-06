package com.example.intranet_back_stage.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NewsResponseDTO {
    private Long id;
    private String title;
    private String imagePath;
    private String description;
    private String content;
    private String ownerUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


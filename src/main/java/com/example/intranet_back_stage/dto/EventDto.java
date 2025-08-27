// src/main/java/.../dto/EventDto.java
package com.example.intranet_back_stage.dto;

import java.time.LocalDateTime;

public record EventDto(
        Long id,
        String title,
        String description,
        String location,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String status,
        String visibility,
        Integer capacity,
        boolean hasCover,
        String coverUrl,          // <— relative path like /uploads/event-covers/xxx.jpg
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

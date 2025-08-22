package com.example.intranet_back_stage.dto;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        String type,
        String title,
        String message,
        String actorUsername,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {}

package com.example.intranet_back_stage.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record EventUpdateDto(
        String title,
        String description,
        String location,
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,
        String visibility,
        Integer capacity,
        String status             // PLANNED|CANCELLED|POSTPONED
) {}
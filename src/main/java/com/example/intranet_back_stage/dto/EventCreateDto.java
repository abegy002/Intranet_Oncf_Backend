// dto/EventCreateDto.java
package com.example.intranet_back_stage.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record EventCreateDto(
        @NotBlank String title,
        String description,
        String location,
        @NotNull OffsetDateTime startsAt,   // <-- accept ISO string with Z or +hh:mm
        OffsetDateTime endsAt,
        Integer capacity,
        String visibility   // INTERNAL / PUBLIC
) {}

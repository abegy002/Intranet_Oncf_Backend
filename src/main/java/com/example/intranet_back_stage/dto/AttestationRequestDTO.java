package com.example.intranet_back_stage.dto;


import com.example.intranet_back_stage.model.AttestationRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AttestationRequestDTO {
    @NotNull
    private Long userId;

    @NotNull
    private AttestationRequest.AttestationType attestationType;
}


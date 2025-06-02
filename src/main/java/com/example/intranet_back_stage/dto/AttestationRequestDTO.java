package com.example.intranet_back_stage.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttestationRequestDTO {
    Long id;

    @NotBlank(message = "Le type d'attestation est obligatoire")
    private String attestationType;
}


package com.example.intranet_back_stage.dto;

public record AttestationRequestResponse(
        Long id,
        String attestationType,
        String status,
        String firstname,
        String lastname
) {}

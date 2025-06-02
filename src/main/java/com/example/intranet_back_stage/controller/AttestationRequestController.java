package com.example.intranet_back_stage.controller;


import com.example.intranet_back_stage.dto.AttestationRequestDTO;
import com.example.intranet_back_stage.enums.Status;
import com.example.intranet_back_stage.model.AttestationRequest;
import com.example.intranet_back_stage.model.User;
import com.example.intranet_back_stage.service.AttestationRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/attestations")
@RequiredArgsConstructor
public class AttestationRequestController {

    private final AttestationRequestService attestationRequestService;

    // User crée une demande
    @PostMapping("/request")
    public ResponseEntity<AttestationRequest> createRequest(@AuthenticationPrincipal User user,
                                                            @RequestBody AttestationRequestDTO dto) {
        AttestationRequest created = attestationRequestService.createRequest(user, dto);
        return ResponseEntity.ok(created);
    }

    // Liste demandes par user
    @GetMapping("/my-requests")
    public ResponseEntity<List<AttestationRequest>> getMyRequests(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(attestationRequestService.getRequestsByUser(user));
    }

    // RH approuve et upload fichier
    @PostMapping(value = "/approve/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> approveRequest(@PathVariable Long id,
                                            @RequestParam("file") MultipartFile file) {
        try {
            Optional<AttestationRequest> updated = attestationRequestService.approveRequest(id, file);
            return updated.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur lors de l'approbation : " + e.getMessage());
        }
    }

    // RH rejette la demande
    @PostMapping("/reject/{id}")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id) {
        Optional<AttestationRequest> rejected = attestationRequestService.rejectRequest(id);
        return rejected.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // User télécharge le fichier d'attestation si approuvé
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> downloadAttestation(@AuthenticationPrincipal User user,
                                                      @PathVariable Long id) {
        Optional<AttestationRequest> optRequest = attestationRequestService.getRequestsByUser(user).stream()
                .filter(req -> req.getId().equals(id) && req.getStatus() == Status.APPROVED)
                .findFirst();

        if(optRequest.isEmpty() || optRequest.get().getAttestationFile() == null) {
            return ResponseEntity.notFound().build();
        }

        AttestationRequest attestation = optRequest.get();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attestation.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(attestation.getFileType()))
                .body(attestation.getAttestationFile());
    }
}


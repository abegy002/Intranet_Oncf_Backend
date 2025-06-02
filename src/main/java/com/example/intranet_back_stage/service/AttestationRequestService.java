package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.AttestationRequestDTO;
import com.example.intranet_back_stage.enums.Status;
import com.example.intranet_back_stage.model.AttestationRequest;
import com.example.intranet_back_stage.model.User;
import com.example.intranet_back_stage.repository.AttestationRequestRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttestationRequestService {

    private final AttestationRequestRepository attestationRequestRepository;

    // User crée une demande d'attestation (statut initial PENDING)
    public AttestationRequest createRequest(User user, AttestationRequestDTO dto) {
        AttestationRequest request = new AttestationRequest();
        request.setUser(user);
        request.setAttestationType(dto.getAttestationType());
        request.setStatus(Status.PENDING);
        request.setRequestDate(java.time.LocalDateTime.now());
        return attestationRequestRepository.save(request);
    }

    // Liste des demandes par user
    public List<AttestationRequest> getRequestsByUser(User user) {
        return attestationRequestRepository.findByUser(user);
    }

    // RH approuve et envoie le fichier attestation
    @Transactional
    public Optional<AttestationRequest> approveRequest(Long requestId, MultipartFile file) throws IOException {
        Optional<AttestationRequest> optRequest = attestationRequestRepository.findById(requestId);
        if(optRequest.isPresent()) {
            AttestationRequest request = optRequest.get();
            request.setStatus(Status.APPROVED);
            request.setAttestationFile(file.getBytes());
            request.setFileName(file.getOriginalFilename());
            request.setFileType(file.getContentType());
            attestationRequestRepository.save(request);
            return Optional.of(request);
        }
        return Optional.empty();
    }

    // RH peut aussi rejeter une demande
    public Optional<AttestationRequest> rejectRequest(Long requestId) {
        Optional<AttestationRequest> optRequest = attestationRequestRepository.findById(requestId);
        if(optRequest.isPresent()) {
            AttestationRequest request = optRequest.get();
            request.setStatus(Status.REJECTED);
            attestationRequestRepository.save(request);
            return Optional.of(request);
        }
        return Optional.empty();
    }
}


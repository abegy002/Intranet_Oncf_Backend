package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.dto.AttestationRequestDTO;
import com.example.intranet_back_stage.dto.AttestationRequestResponse;
import com.example.intranet_back_stage.model.AttestationRequest;
import com.example.intranet_back_stage.service.AttestationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/attestations")
@RequiredArgsConstructor
public class AttestationController {

    private final AttestationService service;

    /* ---------- Create ---------- */
    @PostMapping("/submit")
    public ResponseEntity<AttestationRequest> submit(@Valid @RequestBody AttestationRequestDTO dto) {
        AttestationRequest request = service.submit(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    /* ---------- Read ---------- */
    @GetMapping("/all")
    public ResponseEntity<List<AttestationRequestResponse>> getAll() {
        return ResponseEntity.ok(service.getAllRequests());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AttestationRequest>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(service.getByEmployee(userId));
    }

    /* ---------- Workflow actions ---------- */
    @PutMapping("/process/{id}")
    public ResponseEntity<AttestationRequest> process(@PathVariable Long id,
                                                      @RequestParam String processedBy) {
        return ResponseEntity.ok(service.process(id, processedBy));
    }

    // Upload the scanned & signed PDF and mark as ENVOYE
    @PutMapping(value = "/send/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttestationRequest> sendWithSignedFile(@PathVariable Long id,
                                                                 @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(service.sendWithSignedPdf(id, file));
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<AttestationRequest> reject(@PathVariable Long id) {
        return ResponseEntity.ok(service.reject(id));
    }

    /* ---------- PDF generation (unsigned) ---------- */
    @GetMapping("/generate-pdf/{id}")
    public ResponseEntity<byte[]> generatePdf(@PathVariable Long id) {
        byte[] pdfContent = service.generatePdfAndReturnBytes(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "attestation_" + id + ".pdf");

        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }

    @GetMapping("/signed/{id}")
    public ResponseEntity<Resource> downloadSigned(@PathVariable Long id) {
        Resource file = service.loadSignedPdfAsResource(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"attestation_" + id + "_signed.pdf\"")
                .body(file);
    }
}

package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.dto.LeaveRequestCreateDTO;
import com.example.intranet_back_stage.dto.LeaveRequestRHCreateDTO;
import com.example.intranet_back_stage.dto.LeaveRequestResponse;
import com.example.intranet_back_stage.model.LeaveBalance;
import com.example.intranet_back_stage.model.LeaveRequest;
import com.example.intranet_back_stage.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService service;

    /* ===================== MAPPER ===================== */
    private LeaveRequestResponse toResponse(LeaveRequest r) {
        return new LeaveRequestResponse(
                r.getId(),
                r.getType().name(),
                r.getStatus().name(),
                r.getStartDate(),
                r.getEndDate(),
                r.getReason(),
                r.getJustificatifFilename(),
                r.getCreatedAt(),
                r.getDecidedAt(),
                r.getDecidedBy(),
                r.getEmployee() != null
                        ? (r.getEmployee().getFirstname() + " " + r.getEmployee().getLastname())
                        : null
        );
    }

    /* ===================== COMMANDS ===================== */

    @PostMapping("/employee/create")
    public LeaveRequestResponse createByEmployee(@Valid @RequestBody LeaveRequestCreateDTO dto) {
        return toResponse(service.createByEmployee(dto));
    }

    @PostMapping(value = "/rh/create", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LeaveRequestResponse> createByRh(
            @Valid @RequestPart("payload") LeaveRequestRHCreateDTO payload,
            @RequestPart(value = "file", required = false) MultipartFile file
    ) {
        LeaveRequest created = service.createByRh(payload, file);
        // return 201 + consistent DTO
        return ResponseEntity
                .created(URI.create("/leaves/" + created.getId()))
                .body(toResponse(created));
    }

    @GetMapping("/all")
    public List<LeaveRequestResponse> all() {
        return service.getAllLeaves();
    }

    @PutMapping("/{id}/approve")
    public LeaveRequestResponse approve(@PathVariable Long id, @RequestParam String rh) {
        return toResponse(service.approve(id, rh));
    }

    @PutMapping("/{id}/reject")
    public LeaveRequestResponse reject(@PathVariable Long id,
                                       @RequestParam String rh,
                                       @RequestParam(required = false) String reason) {
        return toResponse(service.reject(id, rh, reason));
    }

    @PutMapping("/{id}/cancel")
    public LeaveRequestResponse cancel(@PathVariable Long id, @RequestParam Long userId) {
        return toResponse(service.cancel(id, userId));
    }

    /* ===================== QUERIES ===================== */

    @GetMapping("/me/{userId}")
    public List<LeaveRequestResponse> mine(@PathVariable Long userId) {
        return service.getMine(userId).stream().map(this::toResponse).toList();
    }

    @GetMapping("/pending")
    public List<LeaveRequestResponse> pending() {
        return service.getAllPending().stream().map(this::toResponse).toList();
    }

    @GetMapping("/balance/{userId}/{year}")
    public LeaveBalance balance(@PathVariable Long userId, @PathVariable int year) {
        return service.getBalance(userId, year);
    }
}

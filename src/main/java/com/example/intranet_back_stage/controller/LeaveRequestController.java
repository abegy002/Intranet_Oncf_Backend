package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.enums.Status;
import com.example.intranet_back_stage.model.LeaveRequest;
import com.example.intranet_back_stage.service.LeaveRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leaves")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    // Employé crée une demande
    @PostMapping("/request/{id}")
    public ResponseEntity<LeaveRequest> createLeaveRequest(
            @PathVariable Long id,
            @RequestBody LeaveRequest request
    ) {
        LeaveRequest saved = leaveRequestService.createRequest(id, request);
        return ResponseEntity.ok(saved);
    }

    // Employé consulte ses demandes
    @GetMapping("/user/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<List<LeaveRequest>> getUserRequests(@PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.getUserRequests(id));
    }

    // HR consulte les demandes en attente
    @GetMapping("/pending")
    public ResponseEntity<List<LeaveRequest>> getPendingRequests() {
        return ResponseEntity.ok(leaveRequestService.getPendingRequests());
    }

    // HR approuve ou refuse une demande
    @PutMapping("/validate/{id}")
    public ResponseEntity<LeaveRequest> validateRequest(
            @PathVariable Long id,
            @RequestParam Status status // "APPROUVÉ" ou "REFUSÉ"
    ) {
        LeaveRequest updated = leaveRequestService.approveOrRejectRequest(id, status);
        return ResponseEntity.ok(updated);
    }
}

package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.dto.LeaveRequestDTO;
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

    // Tous les congés (pour HR)
    @GetMapping("/all")
    public ResponseEntity<List<LeaveRequestDTO>> getAllRequests() {
        return ResponseEntity.ok(leaveRequestService.getAllRequests());
    }

    @PostMapping("/request/{id}")
    public ResponseEntity<LeaveRequestDTO> createLeaveRequest(
            @PathVariable Long id,
            @RequestBody LeaveRequest request
    ) {
        return ResponseEntity.ok(leaveRequestService.createRequest(id, request));
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<LeaveRequestDTO>> getUserRequests(@PathVariable Long id) {
        return ResponseEntity.ok(leaveRequestService.getUserRequests(id));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<LeaveRequestDTO>> getPendingRequests() {
        return ResponseEntity.ok(leaveRequestService.getPendingRequests());
    }

    @PutMapping("/validate/{id}")
    public ResponseEntity<LeaveRequestDTO> validateRequest(
            @PathVariable Long id,
            @RequestParam Status status
    ) {
        System.out.println("✅ Requête reçue : id=" + id + ", status=" + status);
        return ResponseEntity.ok(leaveRequestService.approveOrRejectRequest(id, status));
    }
}

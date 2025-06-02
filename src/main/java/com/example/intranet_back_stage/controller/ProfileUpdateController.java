package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.enums.Status;
import com.example.intranet_back_stage.model.ProfileUpdateRequest;
import com.example.intranet_back_stage.model.User;
import com.example.intranet_back_stage.repository.ProfileUpdateRequestRepository;
import com.example.intranet_back_stage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileUpdateController {

    private final ProfileUpdateRequestRepository requestRepo;
    private final UserRepository userRepo;

    @PostMapping("/update-request")
    public ResponseEntity<?> requestUpdate(@RequestBody ProfileUpdateRequest req, Principal principal) {
        User user = userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        long pendingCount = requestRepo.countByUserAndStatus(user, Status.PENDING);
        if (pendingCount >= 3) {
            return ResponseEntity.badRequest().body("You already have 3 pending requests. Please wait for them to be processed.");
        }

        req.setUser(user);
        req.setStatus(Status.PENDING);
        requestRepo.save(req);

        return ResponseEntity.ok("Modification request submitted for approval.");
    }


    @GetMapping("/pending-requests")
    @PreAuthorize("hasRole('HR')")
    public List<ProfileUpdateRequest> getPendingRequests() {
        return requestRepo.findByStatus(Status.PENDING);
    }

    @PutMapping("/approve/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<?> approveRequest(@PathVariable Long id) {
        ProfileUpdateRequest req = requestRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (req.getStatus() != Status.PENDING)
            return ResponseEntity.badRequest().body("Request already handled");

        User user = req.getUser();
        user.setFirstname(req.getFirstname());
        user.setLastname(req.getLastname());
        user.setEmail(req.getEmail());
        userRepo.save(user);

        req.setStatus(Status.APPROVED);
        requestRepo.save(req);

        return ResponseEntity.ok("Profile update approved and applied");
    }

    @PutMapping("/reject/{id}")
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<?> rejectRequest(@PathVariable Long id, @RequestBody Map<String, String> reason) {
        ProfileUpdateRequest req = requestRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (req.getStatus() != Status.PENDING)
            return ResponseEntity.badRequest().body("Request already handled");

        req.setStatus(Status.REJECTED);
        req.setRejectionReason(reason.get("reason"));
        requestRepo.save(req);

        return ResponseEntity.ok("Request rejected");
    }


}

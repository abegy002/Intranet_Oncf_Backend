package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.enums.Status;
import com.example.intranet_back_stage.model.LeaveRequest;
import com.example.intranet_back_stage.model.User;
import com.example.intranet_back_stage.repository.LeaveRequestRepository;
import com.example.intranet_back_stage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;

    private final UserRepository userRepository;

    public LeaveRequest createRequest(Long id, LeaveRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();
        long requestedDays = ChronoUnit.DAYS.between(start, end) + 1;

        // Récupérer les congés approuvés de cette année
        List<LeaveRequest> approvedLeaves = leaveRequestRepository.findByUserIdAndStatus(id, Status.APPROVED)
                .stream()
                .filter(r -> r.getStartDate().getYear() == LocalDate.now().getYear())
                .toList();

        long totalDaysTaken = approvedLeaves.stream()
                .mapToLong(r -> ChronoUnit.DAYS.between(r.getStartDate(), r.getEndDate()) + 1)
                .sum();

        if ((totalDaysTaken + requestedDays) > 21) {
            throw new IllegalArgumentException("Dépassement du quota de 21 jours de congé annuel.");
        }

        request.setUser(user);
        request.setStatus(Status.PENDING);
        request.setRequestDate(LocalDateTime.now());
        return leaveRequestRepository.save(request);
    }


    public List<LeaveRequest> getUserRequests(Long id) {
        return leaveRequestRepository.findByUserId(id);
    }

    public List<LeaveRequest> getPendingRequests() {
        return leaveRequestRepository.findByStatus(Status.PENDING);
    }

    public LeaveRequest approveOrRejectRequest(Long id, Status newStatus) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        request.setStatus(newStatus); // "APPROUVÉ" ou "REFUSÉ"
        return leaveRequestRepository.save(request);
    }

}


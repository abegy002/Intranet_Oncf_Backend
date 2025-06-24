package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.LeaveRequestDTO;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;

    public List<LeaveRequestDTO> getAllRequests() {
        return leaveRequestRepository.findAll().stream().map(request -> {
            LeaveRequestDTO dto = new LeaveRequestDTO();
            dto.setId(request.getId());
            dto.setType(request.getType());
            dto.setStatus(request.getStatus().name());
            dto.setReason(request.getReason());
            dto.setStartDate(request.getStartDate());
            dto.setEndDate(request.getEndDate());
            dto.setRequestDate(request.getRequestDate());
            dto.setUserId(request.getUser().getId());
            dto.setUserName(request.getUser().getFirstname() + " " + request.getUser().getLastname());
            return dto;
        }).collect(Collectors.toList());
    }

    public LeaveRequestDTO createRequest(Long id, LeaveRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        LocalDate start = request.getStartDate();
        LocalDate end = request.getEndDate();
        long requestedDays = ChronoUnit.DAYS.between(start, end) + 1;

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

        LeaveRequest saved = leaveRequestRepository.save(request);
        return toDTO(saved);
    }

    public List<LeaveRequestDTO> getUserRequests(Long id) {
        return leaveRequestRepository.findByUserId(id).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<LeaveRequestDTO> getPendingRequests() {
        return leaveRequestRepository.findByStatus(Status.PENDING).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public LeaveRequestDTO approveOrRejectRequest(Long id, Status newStatus) {
        LeaveRequest request = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        request.setStatus(newStatus);
        return toDTO(leaveRequestRepository.save(request));
    }

    private LeaveRequestDTO toDTO(LeaveRequest request) {
        return new LeaveRequestDTO(
                request.getId(),
                request.getType(),
                request.getStatus().name(),
                request.getReason(),
                request.getStartDate(),
                request.getEndDate(),
                request.getRequestDate(),
                request.getUser().getId(),
                request.getUser().getFirstname() + " " + request.getUser().getLastname()
        );
    }
}

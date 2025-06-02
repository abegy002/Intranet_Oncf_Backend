package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.enums.Status;
import com.example.intranet_back_stage.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByUserId(Long id);
    List<LeaveRequest> findByStatus(Status status);
    List<LeaveRequest> findByUserIdAndStatus(Long id, Status Status);
}


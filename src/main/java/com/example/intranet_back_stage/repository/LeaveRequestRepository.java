package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.enums.Status;
import com.example.intranet_back_stage.model.LeaveRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long userId);
    List<LeaveRequest> findAllByStatus(LeaveRequest.LeaveStatus status, Sort sort);
}

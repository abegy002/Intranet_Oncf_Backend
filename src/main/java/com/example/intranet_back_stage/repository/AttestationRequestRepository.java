package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.model.AttestationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttestationRequestRepository extends JpaRepository<AttestationRequest, Long> {
    List<AttestationRequest> findByEmployeeId(Long employeeId);
}


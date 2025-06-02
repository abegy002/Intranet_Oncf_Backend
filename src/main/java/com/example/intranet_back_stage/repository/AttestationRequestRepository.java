package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.model.AttestationRequest;
import com.example.intranet_back_stage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttestationRequestRepository extends JpaRepository<AttestationRequest, Long> {
    List<AttestationRequest> findByUser(User user);
}


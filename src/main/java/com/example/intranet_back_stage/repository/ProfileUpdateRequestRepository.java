package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.model.ProfileUpdateRequest;
import com.example.intranet_back_stage.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.intranet_back_stage.enums.Status;

import java.util.List;

@Repository
public interface ProfileUpdateRequestRepository extends JpaRepository<ProfileUpdateRequest, Long> {
    List<ProfileUpdateRequest> findByStatus(Status status);
    boolean existsByUserAndStatus(User user, Status status);
    long countByUserAndStatus(User user, Status status);
}


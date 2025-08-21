package com.example.intranet_back_stage.repository;

import com.example.intranet_back_stage.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable p);
    Page<Notification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(Long recipientId, Pageable p);
    long countByRecipientIdAndReadAtIsNull(Long recipientId);
}

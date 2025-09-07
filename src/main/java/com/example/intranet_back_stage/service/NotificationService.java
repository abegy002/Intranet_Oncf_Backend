package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.NotificationDto;
import com.example.intranet_back_stage.model.Notification;
import com.example.intranet_back_stage.repository.NotificationRepository;
import com.example.intranet_back_stage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Creates, lists, and updates in-app notifications, and pushes real-time messages via WebSocket.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notifRepo;
    private final NotificationGateway gateway; // your WS gateway: sendToUser(username, dto)
    private final UserRepository userRepo;

    /* ======================= DTO mapping ======================= */

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getActorUsername(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }

    /* ======================= Create (single) ======================= */

    /**
     * Core method. If {@code recipientId} is null, it resolves it from {@code recipientUsername}.
     */
    @Transactional
    public NotificationDto createForUser(Long recipientId,
                                         String recipientUsername,
                                         String type,
                                         String title,
                                         String message,
                                         String actorUsername) {

        if (recipientId == null) {
            String finalRecipientUsername = recipientUsername;
            var user = userRepo.findByUsername(recipientUsername)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown user username=" + finalRecipientUsername));
            recipientId = user.getId();
            recipientUsername = user.getUsername(); // normalize
        }

        Notification n = Notification.builder()
                .recipientId(recipientId)
                .recipientUsername(recipientUsername)
                .type(type)
                .title(title)
                .message(message)
                .actorUsername(actorUsername)
                .createdAt(LocalDateTime.now())
                .build();

        n = notifRepo.save(n);

        // Real-time push
        if (recipientUsername != null && !recipientUsername.isBlank()) {
            gateway.sendToUser(recipientUsername, toDto(n));
        }

        return toDto(n);
    }

    /**
     * Convenience overload: create by username only (id will be resolved).
     */
    @Transactional
    public NotificationDto createForUser(String recipientUsername,
                                         String type,
                                         String title,
                                         String message,
                                         String actorUsername) {
        return createForUser(null, recipientUsername, type, title, message, actorUsername);
    }

    /* ======================= Create (fan-out) ======================= */

    /** Fan-out to explicit user IDs. */
    @Transactional
    public void createForUsers(Collection<Long> userIds,
                               String type,
                               String title,
                               String message,
                               String linkUrl,
                               String actorUsername) {
        if (userIds == null || userIds.isEmpty()) return;

        List<Object[]> rows = userRepo.findIdAndUsernameByIdIn(userIds);
        for (Object[] row : rows) {
            Long uid = (Long) row[0];
            String uname = (String) row[1];
            createForUser(uid, uname, type, title, message, actorUsername);
        }
    }

    /** Fan-out to everyone with a single role. */
    @Transactional
    public void createForRole(String roleName,
                              String type,
                              String title,
                              String message,
                              String linkUrl,
                              String actorUsername) {
        List<Object[]> rows = userRepo.findIdAndUsernameByRole(roleName);
        for (Object[] row : rows) {
            Long uid = (Long) row[0];
            String uname = (String) row[1];
            createForUser(uid, uname, type, title, message, actorUsername);
        }
    }

    /** Fan-out to everyone with ANY of the given roles (dedup by user ID). */
    @Transactional
    public void createForRoles(Collection<String> roleNames,
                               String type,
                               String title,
                               String message,
                               String linkUrl,
                               String actorUsername) {
        if (roleNames == null || roleNames.isEmpty()) return;

        List<Object[]> rows = userRepo.findIdAndUsernameByRoles(roleNames);

        Map<Long, String> idToUsername = new LinkedHashMap<>();
        for (Object[] row : rows) {
            Long uid = (Long) row[0];
            String uname = (String) row[1];
            idToUsername.putIfAbsent(uid, uname);
        }

        for (Map.Entry<Long, String> e : idToUsername.entrySet()) {
            createForUser(e.getKey(), e.getValue(), type, title, message, actorUsername);
        }
    }

    /** Convenience for common case: notify both HR and ADMIN. */
    public void createForHrAndAdmin(String type,
                                    String title,
                                    String message,
                                    String linkUrl,
                                    String actorUsername) {
        createForRoles(Arrays.asList("HR", "ADMIN"), type, title, message, linkUrl, actorUsername);
    }

    /* ======================= Query & update ======================= */

    public Page<NotificationDto> listForUser(Long recipientId, boolean unreadOnly, int page, int size) {
        Pageable p = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Notification> res = unreadOnly
                ? notifRepo.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(recipientId, p)
                : notifRepo.findByRecipientIdOrderByCreatedAtDesc(recipientId, p);
        return res.map(this::toDto);
    }

    @Transactional
    public void markRead(Long id, Long userId) {
        Notification n = notifRepo.findById(id).orElseThrow();
        if (!n.getRecipientId().equals(userId)) {
            throw new IllegalArgumentException("Forbidden");
        }
        if (n.getReadAt() == null) {
            n.setReadAt(LocalDateTime.now());
            notifRepo.save(n);
        }
    }

    @Transactional
    public void markAllRead(Long userId) {
        notifRepo.findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(userId, Pageable.unpaged())
                .forEach(n -> {
                    n.setReadAt(LocalDateTime.now());
                    notifRepo.save(n);
                });
    }

    public long unreadCount(Long userId) {
        return notifRepo.countByRecipientIdAndReadAtIsNull(userId);
    }
}

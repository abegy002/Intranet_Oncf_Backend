package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationGateway {
    private final SimpMessagingTemplate template;

    /** Send to one user (username) => /user/{username}/queue/notifications */
    public void sendToUser(String username, NotificationDto dto) {
        // destination: "/user/queue/notifications"
        template.convertAndSendToUser(username, "/queue/notifications", dto);
    }
}

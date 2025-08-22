package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.dto.NotificationDto;
import com.example.intranet_back_stage.repository.UserRepository;
import com.example.intranet_back_stage.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;
    private final UserRepository userRepo; // inject here, not per-method

    @GetMapping("/my")
    public Page<NotificationDto> myNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Principal p
    ) {
        var user = userRepo.findByUsername(p.getName()).orElseThrow();
        return service.listForUser(user.getId(), unreadOnly, page, size);
    }

    @GetMapping("/my/unread-count")
    public long myUnreadCount(Principal p) {
        var user = userRepo.findByUsername(p.getName()).orElseThrow();
        return service.unreadCount(user.getId());
    }

    @PatchMapping("/{id}/read")
    public void markRead(@PathVariable Long id, Principal p) {
        var user = userRepo.findByUsername(p.getName()).orElseThrow();
        service.markRead(id, user.getId());
    }

    @PostMapping("/read-all")
    public void markAllRead(Principal p) {
        var user = userRepo.findByUsername(p.getName()).orElseThrow();
        service.markAllRead(user.getId());
    }
}

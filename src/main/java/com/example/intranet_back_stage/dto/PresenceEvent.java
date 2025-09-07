package com.example.intranet_back_stage.dto;


import com.example.intranet_back_stage.enums.UserStatus;
import java.time.Instant;

public record PresenceEvent(Long userId, String username, UserStatus status, Instant lastSeen) {}

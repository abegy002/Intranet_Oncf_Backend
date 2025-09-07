package com.example.intranet_back_stage.security;

import com.example.intranet_back_stage.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Duration;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class PresenceScheduler {
    private final UserService userService;

    // every minute mark stale users
    @Scheduled(fixedDelay = 60_000)
    public void sweepPresence() {
        userService.markStaleUsers(
                Duration.ofMinutes(3),  // -> AWAY after 3 min idle
                Duration.ofMinutes(10)  // -> OFFLINE after 10 min idle
        );
    }
}

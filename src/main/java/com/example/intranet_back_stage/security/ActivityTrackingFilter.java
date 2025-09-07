package com.example.intranet_back_stage.security;


import com.example.intranet_back_stage.model.User;
import com.example.intranet_back_stage.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(200)
@RequiredArgsConstructor
public class ActivityTrackingFilter extends OncePerRequestFilter {
    private final UserService userService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User u) {
                userService.heartbeat(u); // updates lastSeen, keeps ONLINE if needed
            }
        } catch (Exception ignored) {}
        chain.doFilter(req, res);
    }
}

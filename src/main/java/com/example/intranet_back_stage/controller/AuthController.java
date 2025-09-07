// src/main/java/.../controller/AuthController.java
package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.dto.UserResponseDTO;
import com.example.intranet_back_stage.security.JwtUtil;
import com.example.intranet_back_stage.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        String access = jwtUtil.generateAccessToken(loginRequest.getUsername());
        String refresh = jwtUtil.generateRefreshToken(loginRequest.getUsername());

        // HttpOnly cookie for refresh token
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", refresh)
                .httpOnly(true)
                .secure(false)               // set true if HTTPS
                .path("/auth")               // cookie sent to /auth/* (refresh/logout)
                .sameSite("Lax")
                .maxAge(7 * 24 * 60 * 60)    // 7 days
                .build();

        UserResponseDTO user = userService.getUserByUsername(loginRequest.getUsername());

        Map<String, Object> body = new HashMap<>();
        body.put("token", access);
        body.put("user", user);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(body);
    }

    @GetMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(value = "refreshToken", required = false) String refreshToken) {
        if (refreshToken == null || !jwtUtil.validateRefreshToken(refreshToken)) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }
        String username = jwtUtil.extractUsernameFromRefresh(refreshToken);

        // issue a NEW access token
        String newAccess = jwtUtil.generateAccessToken(username);
        // (optional) rotate refresh token
        String newRefresh = jwtUtil.generateRefreshToken(username);
        ResponseCookie rotated = ResponseCookie.from("refreshToken", newRefresh)
                .httpOnly(true).secure(false).path("/auth").sameSite("Lax")
                .maxAge(7 * 24 * 60 * 60).build();

        Map<String, Object> body = new HashMap<>();
        body.put("token", newAccess);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, rotated.toString())
                .body(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // clear cookie
        ResponseCookie clear = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(false).path("/auth").sameSite("Lax")
                .maxAge(0).build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, clear.toString()).build();
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    static class LoginRequest {
        private String username;
        private String password;
    }
}

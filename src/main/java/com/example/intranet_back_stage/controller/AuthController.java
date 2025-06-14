package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.dto.UserResponseDTO;
import com.example.intranet_back_stage.model.User;
import com.example.intranet_back_stage.repository.UserRepository;
import com.example.intranet_back_stage.security.JwtUtil;
import com.example.intranet_back_stage.service.UserService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private AuthenticationManager authManager;

    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(), loginRequest.getPassword()
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtil.generateToken(loginRequest.getUsername());

            UserResponseDTO user = userService.getUserByUsername(loginRequest.getUsername());


            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("user", user); // Or use a DTO here

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }
    }

    private JwtUtil jwtUtil;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    class JwtResponse {
        private String token;
    }

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class LoginRequest {
    private String username;
    private String password;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class AuthResponse {
    private User user;
}

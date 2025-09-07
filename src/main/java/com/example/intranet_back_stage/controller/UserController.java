package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.dto.ChangePasswordRequest;
import com.example.intranet_back_stage.dto.UserDTO;
import com.example.intranet_back_stage.dto.UserResponseDTO;
import com.example.intranet_back_stage.enums.UserStatus;
import com.example.intranet_back_stage.model.Permission;
import com.example.intranet_back_stage.model.User;
import com.example.intranet_back_stage.service.RoleService;
import com.example.intranet_back_stage.service.UserService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RoleService roleService;

    @PostMapping("/create")
    public ResponseEntity<?> createUser(@RequestBody UserDTO userDTO) {
        try {
            userService.createUserWithRole(userDTO);
            return ResponseEntity.ok(Map.of("message", "User created successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/permissions")
    public ResponseEntity<List<Permission>> getUserPermissions(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserPermissions(id)); // Doit retourner des Permission
    }

    @PostMapping("/{id}/permissions")
    public ResponseEntity<?> addPermissionToUser(@PathVariable Long id, @RequestBody Permission permission) {
        userService.addPermissionToUser(id, permission);
        return ResponseEntity.ok(Map.of("message", "Permission added successfully"));
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    public ResponseEntity<?> removePermissionFromUser(@PathVariable Long id, @PathVariable Long permissionId) {
        userService.removePermissionFromUser(id, permissionId);
        return ResponseEntity.ok(Map.of("message", "Permission removed successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUserProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(userService.getUserById(user.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        userService.updateUser(id, userDTO);
        return ResponseEntity.ok(Map.of("message", "User updated successfully!"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully!"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal User authUser,
                                            @Valid @RequestBody ChangePasswordRequest body) {
        try {
            userService.changePassword(authUser.getId(), body.getCurrentPassword(), body.getNewPassword());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 204, matches Observable<void>
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PostMapping("/me/heartbeat")
    public ResponseEntity<Void> heartbeat(@AuthenticationPrincipal User authUser) {
        userService.heartbeat(authUser);         // now null-safe in service
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/status")
    public ResponseEntity<Void> setMyStatus(@AuthenticationPrincipal User authUser,
                                            @RequestBody UpdateStatusRequest req) {
        userService.setCurrentUserStatus(authUser, req.getStatus());
        return ResponseEntity.noContent().build();
    }

    @Data
    public static class UpdateStatusRequest {
        private UserStatus status;
    }
}

package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.dto.UserDTO;
import com.example.intranet_back_stage.dto.UserResponseDTO;
import com.example.intranet_back_stage.model.Role;
import com.example.intranet_back_stage.service.RoleService;
import com.example.intranet_back_stage.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
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
    public ResponseEntity<Map<String, String>> createUser(@RequestBody UserDTO userDTO) {
        userService.createUserWithRole(userDTO);
        Map<String, String> response = new HashMap<>();
        response.put("message", "User created successfully");
        return ResponseEntity.ok(response);
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
        return ResponseEntity.ok("User updated successfully!");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("User deleted successfully!");
    }

    @GetMapping("/roles")
    public ResponseEntity<List<Role>> listRoles(Model model) {
        model.addAttribute("roles", roleService.findAll());
        return ResponseEntity.ok(roleService.findAll()); // roles.html
    }

    @PostMapping("/roles/create")
    public String createRole(@RequestParam String roleName) {
        roleService.createRole(roleName);
        return "redirect:/admin/roles";
    }

    @PostMapping("/roles/edit")
    public String editRole(@RequestParam Long roleId, @RequestParam String newRoleName) {
        roleService.updateRole(roleId, newRoleName);
        return "redirect:/admin/roles";
    }

    @PostMapping("/roles/delete")
    public String deleteRole(@RequestParam Long roleId) {
        roleService.deleteRole(roleId);
        return "redirect:/admin/roles";
    }
}

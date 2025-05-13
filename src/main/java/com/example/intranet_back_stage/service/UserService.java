package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.model.Permission;
import com.example.intranet_back_stage.model.Role;
import com.example.intranet_back_stage.model.User;
import com.example.intranet_back_stage.repository.PermissionRepository;
import com.example.intranet_back_stage.repository.RoleRepository;
import com.example.intranet_back_stage.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;

    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepo, RoleRepository roleRepo, PermissionRepository permRepo) {
        this.passwordEncoder = passwordEncoder;
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.permRepo = permRepo;
    }

    public void addPermissionsToRole(String roleName, List<String> permissionNames) {
        Role role = roleRepo.findByName(roleName).orElseThrow(() -> new RuntimeException("Role not found"));

        List<Permission> permissions = permRepo.findByNameIn(permissionNames);
        role.getPermissions().addAll(permissions);
        roleRepo.save(role);
    }

    @Transactional
    public void addRoleToUser(String username, String roleName) {
        // Find the user by their username
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Find the role by its name
        Role role = roleRepo.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        // Assign the role to the user
//        user.getRole().add(role);

        // Save the user with the new role
        userRepo.save(user);
    }

    public Set<Permission> getPermissionsForRole(String roleName) {
        return roleRepo.findByName(roleName).orElseThrow().getPermissions();
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }


    public void createUserWithRole(String username, String password, String roleName) {
        // Check if user already exists
        if (userRepo.findByUsername(username).isPresent()) {
            throw new RuntimeException("User already exists with username: " + username);
        }

        // Find role
        Role role = roleRepo.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));


        // Create and save user
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
//        user.getRole().add(role); // assuming roles is a Set<Role>

        userRepo.save(user);
    }

}


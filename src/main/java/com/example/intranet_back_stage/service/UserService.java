package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.UserDTO;
import com.example.intranet_back_stage.dto.UserResponseDTO;
import com.example.intranet_back_stage.model.*;
import com.example.intranet_back_stage.repository.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional
public class UserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PermissionRepository permRepo;
    private final DepartmentRepository departmentRepo;
    private final JobRepository jobRepo;

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

    /*USERS -------------------------------------------------------------------------------------------- CRUD*/

    public void createUserWithRole(UserDTO userDTO) {
        if (userRepo.findByUsername(userDTO.getUsername()).isPresent()) {
            throw new RuntimeException("User already exists");
        }

        Role role = roleRepo.findByName(userDTO.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role not found"));
        Job job = jobRepo.findById(userDTO.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setFirstname(userDTO.getFirstname());
        user.setLastname(userDTO.getLastname());
        user.setEmail(userDTO.getEmail());
        user.setJob(job);
        user.setRole(role);

        userRepo.save(user);
    }

    public UserResponseDTO getUserById(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapToDTO(user);
    }

    public List<UserResponseDTO> getAllUsers() {
        return userRepo.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public void updateUser(Long id, UserDTO dto) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setFirstname(dto.getFirstname());
        user.setLastname(dto.getLastname());
        user.setEmail(dto.getEmail());
        user.setJob(jobRepo.findById(dto.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found")));
        user.setRole(roleRepo.findByName(dto.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role not found")));

        userRepo.save(user);
    }

    public void deleteUser(Long id) {
        userRepo.deleteById(id);
    }

    private UserResponseDTO mapToDTO(User user) {
        String jobTitle = user.getJob() != null ? user.getJob().getTitle() : null;
        String dept = (user.getJob() != null && user.getJob().getDepartment() != null)
                ? user.getJob().getDepartment().getName()
                : null;

        return new UserResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                jobTitle,
                dept,
                user.getRole() != null ? user.getRole().getName() : null
        );
    }

}


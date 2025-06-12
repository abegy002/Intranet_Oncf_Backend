package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.*;
import com.example.intranet_back_stage.model.*;
import com.example.intranet_back_stage.repository.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
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
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFirstname(user.getFirstname());
        dto.setLastname(user.getLastname());
        dto.setEmail(user.getEmail());

        if (user.getJob() != null) {
            JobDTO jobDTO = new JobDTO();
            jobDTO.setId(user.getJob().getId());
            jobDTO.setTitle(user.getJob().getTitle());
            Department department = user.getJob().getDepartment();
            if (department != null) {
                DepartmentDTO deptDTO = new DepartmentDTO();
                deptDTO.setId(department.getId());
                deptDTO.setName(department.getName());
                jobDTO.setDepartment(deptDTO);
            }
            dto.setJob(jobDTO);
        }


        // map role entity to role DTO
        if (user.getRole() != null) {
            RoleDTO roleDTO = new RoleDTO();
            roleDTO.setId(user.getRole().getId());
            roleDTO.setName(user.getRole().getName());
            dto.setRole(roleDTO);
        }

        return dto;
    }


}


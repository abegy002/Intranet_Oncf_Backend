package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.*;
import com.example.intranet_back_stage.enums.UserStatus;
import com.example.intranet_back_stage.model.*;
import com.example.intranet_back_stage.repository.*;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    private final SimpMessagingTemplate messagingTemplate;

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
        user.setSalaire(userDTO.getSalary());
        user.setHireDate(userDTO.getHireDate());
        user.setCin(userDTO.getCin());
        user.setPhoneNumber(userDTO.getPhoneNumber());

        String code;
        do {
            // Generate random alphanumeric string of 6 characters
            code = "EMP-" + UUID.randomUUID().toString()
                    .replaceAll("[^A-Za-z0-9]", "") // remove dashes and keep only alphanumerics
                    .substring(0, 6)                // take first 6 characters
                    .toUpperCase();                 // make it uppercase
        } while (userRepo.existsByEmployeeCode(code));

        user.setEmployeeCode(code);


        user.setJob(job);
        user.setRole(role);

        // Assign permissions directly from DTO
        if (userDTO.getPermissions() != null) {
            List<Permission> permissions = userDTO.getPermissions().stream()
                    .map(name -> permRepo.findByName(name)
                            .orElseThrow(() -> new RuntimeException("Permission not found: " + name)))
                    .toList();
            user.setPermissions(new HashSet<>(permissions));
        } else {
            user.setPermissions(new HashSet<>());
        }

        userRepo.save(user);
    }

    public UserResponseDTO getUserById(Long id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return mapToDTO(user);
    }

    public UserResponseDTO getUserByUsername(String username) {
        User user = userRepo.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
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
        user.setSalaire(dto.getSalary());
        // ... inside updateUser(Long id, UserDTO dto)
        user.setHireDate(dto.getHireDate());
        user.setCin(dto.getCin());
        user.setPhoneNumber(dto.getPhoneNumber());


        Job job = jobRepo.findById(dto.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found"));
        user.setJob(job);

        Role role = roleRepo.findByName(dto.getRoleName())
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRole(role);

        // Replace old permissions with new set
        if (dto.getPermissions() != null) {
            List<Permission> permissions = dto.getPermissions().stream()
                    .map(name -> permRepo.findByName(name)
                            .orElseThrow(() -> new RuntimeException("Permission not found: " + name)))
                    .toList();
            user.setPermissions(new HashSet<>(permissions));
        } else {
            user.getPermissions().clear();
        }

        userRepo.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepo.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepo.deleteById(id);
    }

    private UserResponseDTO mapToDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setEmployeeCode(user.getEmployeeCode());
        dto.setUsername(user.getUsername());
        dto.setFirstname(user.getFirstname());
        dto.setLastname(user.getLastname());
        dto.setEmail(user.getEmail());
        dto.setSalary(user.getSalaire());
        dto.setHireDate(user.getHireDate());
        dto.setCin(user.getCin());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setStatus(user.getStatus());
        dto.setLastSeen(user.getLastSeen());

        // Job
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

        // Role
        if (user.getRole() != null) {
            RoleDTO roleDTO = new RoleDTO();
            roleDTO.setId(user.getRole().getId());
            roleDTO.setName(user.getRole().getName());
            dto.setRole(roleDTO);
        }

        // Set permissions directly
        dto.setPermissions(user.getPermissions().stream()
                .map(Permission::getName)
                .toList());

        return dto;
    }

    public List<Permission> getUserPermissions(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new ArrayList<>(user.getPermissions()); // ou getRole().getPermissions() si héritées
    }

    public void addPermissionToUser(Long userId, Permission permission) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Permission perm = permRepo.findById(permission.getId())
                .orElseThrow(() -> new RuntimeException("Permission not found"));

        user.getPermissions().add(perm);
        userRepo.save(user);
    }

    public void removePermissionFromUser(Long userId, Long permissionId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.getPermissions().removeIf(p -> p.getId().equals(permissionId));
        userRepo.save(user);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        if (!StringUtils.hasText(currentPassword) || !StringUtils.hasText(newPassword)) {
            throw new RuntimeException("Both current and new passwords are required");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Prevent reusing the same password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new RuntimeException("New password must be different from the current password");
        }

        // (Optional) basic strength check; keep if you want an extra guard
        // if (!newPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).+$")) {
        //     throw new RuntimeException("Password must contain upper, lower, digit and special character");
        // }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    // ... keep all your existing methods

    public void setCurrentUserStatus(User authUser, UserStatus status) {
        if (authUser == null) return; // null-safe
        User u = userRepo.findById(authUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        u.setStatus(status);
        u.setLastSeen(LocalDateTime.now());
        userRepo.save(u);
        publishPresence(u);            // <- notify subscribers
    }

    /** Null-safe heartbeat (can be called from filter or endpoint) */
    public void heartbeat(User authUser) {
        if (authUser == null) return;  // prevent NPE when anonymous
        User u = userRepo.findById(authUser.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        u.setLastSeen(LocalDateTime.now());
        if (u.getStatus() != UserStatus.ONLINE) {
            u.setStatus(UserStatus.ONLINE);
        }
        userRepo.save(u);
        publishPresence(u);            // <- notify subscribers
    }

    /** Mark users AWAY/OFFLINE if stale and broadcast changes */
    public int markStaleUsers(Duration awayAfter, Duration offlineAfter) {
        LocalDateTime now = LocalDateTime.now();
        List<User> all = userRepo.findAll();
        int changed = 0;
        for (User u : all) {
            LocalDateTime ls = u.getLastSeen();
            UserStatus next = u.getStatus();
            if (ls == null) {
                next = UserStatus.OFFLINE;
            } else {
                var since = Duration.between(ls, now);
                if (since.compareTo(offlineAfter) > 0) {
                    next = UserStatus.OFFLINE;
                } else if (since.compareTo(awayAfter) > 0) {
                    next = UserStatus.AWAY;
                } else {
                    next = UserStatus.ONLINE;
                }
            }
            if (next != u.getStatus()) {
                u.setStatus(next);
                userRepo.save(u);
                publishPresence(u);    // <- notify subscribers
                changed++;
            }
        }
        return changed;
    }

    private void publishPresence(User u) {
        PresenceEvent evt = new PresenceEvent(
                u.getId(),
                u.getUsername(),
                u.getStatus(),
                u.getLastSeen() == null ? null : u.getLastSeen().atZone(java.time.ZoneId.systemDefault()).toInstant()
        );
        messagingTemplate.convertAndSend("/topic/presence", evt);
    }
}

package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.RoleDTO;
import com.example.intranet_back_stage.mapper.RoleMapper;
import com.example.intranet_back_stage.model.Permission;
import com.example.intranet_back_stage.model.Role;
import com.example.intranet_back_stage.model.User;
import com.example.intranet_back_stage.repository.PermissionRepository;
import com.example.intranet_back_stage.repository.RoleRepository;
import com.example.intranet_back_stage.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;

    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(roleMapper::toDTO)
                .collect(Collectors.toList());
    }

    public RoleDTO getRoleById(Long id) {
        return roleRepository.findById(id)
                .map(roleMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Role not found"));
    }

    public RoleDTO createRole(RoleDTO roleDTO) {
        Role role = roleMapper.toEntity(roleDTO);
        return roleMapper.toDTO(roleRepository.save(role));
    }

    public RoleDTO updateRole(Long id, RoleDTO roleDTO) {
        Role existing = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        existing.setName(roleDTO.getName());
        existing.setPermissions(roleMapper.toEntity(roleDTO).getPermissions());
        return roleMapper.toDTO(roleRepository.save(existing));
    }

    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }

    public RoleDTO assignPermissionsToRole(Long roleId, Set<Long> permissionIds) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found with id: " + roleId));

        Set<Permission> permissions = permissionIds.stream()
                .map(id -> permissionRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Permission not found with id: " + id)))
                .collect(Collectors.toSet());

        role.setPermissions(permissions);
        Role savedRole = roleRepository.save(role);
        return roleMapper.toDTO(savedRole);
    }

    @Transactional
    public void addRoleToUser(String username, String roleName) {
        // Find the user by their username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Find the role by its name
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found"));

        // Assign the role to the user
//        user.getRole().add(role);

        // Save the user with the new role
        userRepository.save(user);
    }

}

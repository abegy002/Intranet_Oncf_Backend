package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.PermissionDTO;
import com.example.intranet_back_stage.dto.RoleDTO;
import com.example.intranet_back_stage.model.Permission;
import com.example.intranet_back_stage.model.Role;
import com.example.intranet_back_stage.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    // Convert Permission entity to PermissionDTO
    private PermissionDTO toDTO(Permission permission) {
        PermissionDTO dto = new PermissionDTO();
        dto.setId(permission.getId());
        dto.setName(permission.getName());
        return dto;
    }

    // Convert PermissionDTO to Permission entity
    private Permission toEntity(PermissionDTO dto) {
        Permission permission = new Permission();
        permission.setId(dto.getId());
        permission.setName(dto.getName());
        return permission;
    }

    public List<PermissionDTO> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PermissionDTO getPermissionById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found"));
        return toDTO(permission);
    }

    public PermissionDTO createPermission(PermissionDTO dto) {
        Permission permission = new Permission();
        permission.setName(dto.getName());
        Permission saved = permissionRepository.save(permission);
        return new PermissionDTO(saved.getId(), saved.getName());
    }

    public PermissionDTO updatePermission(Long id, PermissionDTO dto) {
        Permission existing = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found"));

        existing.setName(dto.getName()); // Only name is updatable
        Permission updated = permissionRepository.save(existing);
        return toDTO(updated);
    }

    public void deletePermission(Long id) {
        permissionRepository.deleteById(id);
    }
}

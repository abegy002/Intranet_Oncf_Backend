package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.PermissionDTO;
import com.example.intranet_back_stage.mapper.PermissionMapper;
import com.example.intranet_back_stage.model.Permission;
import com.example.intranet_back_stage.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    public List<PermissionDTO> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(permissionMapper::toDTO)
                .collect(Collectors.toList());
    }

    public PermissionDTO getPermissionById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found"));
        return permissionMapper.toDTO(permission);
    }

    public PermissionDTO createPermission(PermissionDTO dto) {
        Permission entity = permissionMapper.toEntity(dto);
        Permission saved = permissionRepository.save(entity);
        return permissionMapper.toDTO(saved);
    }

    public PermissionDTO updatePermisson(Long id, PermissionDTO dto) {
        Permission existing = permissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found"));

        existing.setName(dto.getName()); // Only name is updatable
        Permission updated = permissionRepository.save(existing);
        return permissionMapper.toDTO(updated);
    }

    public void deletePermission(Long id) {
        permissionRepository.deleteById(id);
    }
}

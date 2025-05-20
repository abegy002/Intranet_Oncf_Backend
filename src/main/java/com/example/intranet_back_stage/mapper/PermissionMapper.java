package com.example.intranet_back_stage.mapper;

import com.example.intranet_back_stage.dto.PermissionDTO;
import com.example.intranet_back_stage.model.Permission;
import com.example.intranet_back_stage.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionMapper {

    private final PermissionRepository permissionRepository;

    public Permission toEntity(PermissionDTO dto) {
        Permission permission = new Permission();
        permission.setId(dto.getId());
        permission.setName(dto.getName());
        return permission;
    }

    public PermissionDTO toDTO(Permission permission) {
        PermissionDTO dto = new PermissionDTO();
        dto.setId(permission.getId());
        dto.setName(permission.getName());
        return dto;
    }
}

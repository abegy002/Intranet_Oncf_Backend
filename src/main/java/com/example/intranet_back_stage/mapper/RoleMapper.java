package com.example.intranet_back_stage.mapper;

import com.example.intranet_back_stage.dto.RoleDTO;
import com.example.intranet_back_stage.model.Permission;
import com.example.intranet_back_stage.model.Role;
import com.example.intranet_back_stage.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RoleMapper {
    private final PermissionRepository permissionRepository;

    public Role toEntity(RoleDTO dto) {
        Role role = new Role();
        role.setId(dto.getId());
        role.setName(dto.getName());
        if (dto.getPermissionIds() != null) {
            role.setPermissions(dto.getPermissionIds().stream()
                    .map(permissionRepository::findById)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toSet()));
        }
        return role;
    }

    public RoleDTO toDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setPermissionIds(role.getPermissions().stream()
                .map(Permission::getId)
                .collect(Collectors.toSet()));
        return dto;
    }
}


package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.RoleDTO;
import com.example.intranet_back_stage.model.Role;
import com.example.intranet_back_stage.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public RoleDTO createRole(RoleDTO dto) {
        Role role = new Role();
        role.setName(dto.getName());
        Role saved = roleRepository.save(role);
        return new RoleDTO(saved.getId(), saved.getName());
    }

    public List<RoleDTO> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(role -> new RoleDTO(role.getId(), role.getName()))
                .collect(Collectors.toList());
    }

    public RoleDTO getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        return new RoleDTO(role.getId(), role.getName());
    }

    public RoleDTO updateRole(Long id, RoleDTO dto) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        role.setName(dto.getName());
        role = roleRepository.save(role);
        return new RoleDTO(role.getId(), role.getName());
    }

    public void deleteRole(Long id) {
        roleRepository.deleteById(id);
    }
}

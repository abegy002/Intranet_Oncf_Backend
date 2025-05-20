package com.example.intranet_back_stage.dto;

import lombok.Data;

import java.util.Set;

@Data
public class RoleDTO {
    private Long id;
    private String name;
    private Set<Long> permissionIds; // Only IDs for simplification
}

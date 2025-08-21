package com.example.intranet_back_stage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String employeeCode;
    private String username;
    private String firstname;
    private String lastname;
    private String email;
    private BigDecimal salary;
    private JobDTO job;
    private RoleDTO role;  // nested role object
    private List<String> permissions;
}

package com.example.intranet_back_stage.dto;

import com.example.intranet_back_stage.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private LocalDate hireDate;
    private String cin;
    private String phoneNumber;
    // presence
    private UserStatus status;           // ONLINE / AWAY / OFFLINE
    private LocalDateTime lastSeen;
    private JobDTO job;
    private RoleDTO role;  // nested role object
    private List<String> permissions;
}

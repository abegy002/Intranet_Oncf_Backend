package com.example.intranet_back_stage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobDTO {
    private Long id;

    @NotBlank(message = "Job title is required")
    private String title;
    private DepartmentDTO department;
}

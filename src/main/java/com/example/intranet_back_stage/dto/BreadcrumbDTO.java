// BreadcrumbDTO.java
package com.example.intranet_back_stage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BreadcrumbDTO {
    private Long id;
    private String name;
    private String type; // "space" or "folder"
}

// CreateSpaceRequest.java
package com.example.intranet_back_stage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSpaceRequest {
    private String name;
    private Long userId;
    private String description;
}

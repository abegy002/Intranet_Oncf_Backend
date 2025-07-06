package com.example.intranet_back_stage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNewsRequest {
    public String title;
    public String description;
    public String content;
}
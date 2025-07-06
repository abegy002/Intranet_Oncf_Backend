package com.example.intranet_back_stage.dto;


import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateNewsRequest {
    private String title;
    private String description;
    private String content;
    private Long ownerId;
    private MultipartFile image;
}

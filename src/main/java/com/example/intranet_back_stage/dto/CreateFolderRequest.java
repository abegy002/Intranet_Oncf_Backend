package com.example.intranet_back_stage.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateFolderRequest {
    private String name;
    private String description;
    private Long parentFolderId;
    private Long documentSpaceId;
}
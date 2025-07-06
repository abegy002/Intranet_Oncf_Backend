package com.example.intranet_back_stage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentFolderDTO {
    private Long id;
    private String name;
    private String description;
    private Long parentFolderId;
    private Long documentSpaceId;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DocumentFolderDTO> subFolders;
    private List<DocumentDTO> documents;
    private int documentsCount;
    private int subFoldersCount;
}

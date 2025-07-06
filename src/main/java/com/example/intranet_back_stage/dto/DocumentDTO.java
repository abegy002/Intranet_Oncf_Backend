// DocumentDTO.java
package com.example.intranet_back_stage.dto;

import com.example.intranet_back_stage.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentDTO {
    private Long id;
    private String name;
    private String originalName;
    private String filePath;
    private Long fileSize;
    private String mimeType;
    private DocumentType documentType;
    private String description;
    private Long folderId;
    private Long documentSpaceId;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
    private String downloadUrl;
}

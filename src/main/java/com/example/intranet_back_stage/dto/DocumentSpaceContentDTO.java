// DocumentSpaceContentDTO.java
package com.example.intranet_back_stage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentSpaceContentDTO {
    private Long spaceId;
    private String spaceName;
    private Long currentFolderId;
    private String currentFolderName;
    private List<BreadcrumbDTO> breadcrumbs;
    private List<DocumentFolderDTO> folders;
    private List<DocumentDTO> documents;
}

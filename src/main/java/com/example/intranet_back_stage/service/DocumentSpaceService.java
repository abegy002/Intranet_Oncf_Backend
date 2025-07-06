package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.*;
import com.example.intranet_back_stage.model.Document;
import com.example.intranet_back_stage.model.DocumentFolder;
import com.example.intranet_back_stage.model.DocumentSpace;
import com.example.intranet_back_stage.model.User;
import com.example.intranet_back_stage.repository.DocumentFolderRepository;
import com.example.intranet_back_stage.repository.DocumentRepository;
import com.example.intranet_back_stage.repository.DocumentSpaceRepository;
import com.example.intranet_back_stage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentSpaceService {

    private final DocumentSpaceRepository documentSpaceRepository;
    private final DocumentRepository documentRepository;
    private final DocumentFolderRepository documentFolderRepository;
    private final UserRepository userRepository;

    // CREATE
    public DocumentSpaceDTO createDocumentSpace(String name, String description, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        DocumentSpace space = new DocumentSpace();
        space.setName(name);
        space.setDescription(description);
        space.setCreatedBy(user);

        DocumentSpace saved = documentSpaceRepository.save(space);
        return convertToDTO(saved);
    }

    // READ (all)
    public List<DocumentSpaceDTO> getAllDocumentSpaces() {
        return documentSpaceRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // READ (by ID)
    public DocumentSpaceDTO getDocumentSpaceById(Long id) {
        DocumentSpace space = documentSpaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document space not found"));
        return convertToDTO(space);
    }

    // UPDATE
    public DocumentSpaceDTO updateDocumentSpace(Long id, String name, String description) {
        DocumentSpace space = documentSpaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document space not found"));

        space.setName(name);
        space.setDescription(description);
        DocumentSpace updated = documentSpaceRepository.save(space);
        return convertToDTO(updated);
    }

    // DELETE
    public void deleteDocumentSpace(Long id) {
        DocumentSpace space = documentSpaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document space not found"));

        documentSpaceRepository.delete(space);
    }

    public DocumentSpaceContentDTO getSpaceContent(Long spaceId, Long folderId) {
        DocumentSpace space = documentSpaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Document space not found"));

        DocumentSpaceContentDTO content = new DocumentSpaceContentDTO();
        content.setSpaceId(spaceId);
        content.setSpaceName(space.getName());

        List<BreadcrumbDTO> breadcrumbs = new ArrayList<>();
        breadcrumbs.add(new BreadcrumbDTO(spaceId, space.getName(), "space"));

        if (folderId != null) {
            DocumentFolder folder = documentFolderRepository.findById(folderId)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));

            content.setCurrentFolderId(folderId);
            content.setCurrentFolderName(folder.getName());

            // Build breadcrumbs
            buildBreadcrumbs(folder, breadcrumbs);

            // Get folder contents
            content.setFolders(documentFolderRepository.findByParentFolderId(folderId)
                    .stream()
                    .map(this::convertToFolderDTO)
                    .collect(Collectors.toList()));

            content.setDocuments(documentRepository.findByFolderId(folderId)
                    .stream()
                    .map(this::convertToDocumentDTO)
                    .collect(Collectors.toList()));
        } else {
            // Root level content
            content.setFolders(documentFolderRepository.findRootFoldersBySpaceId(spaceId)
                    .stream()
                    .map(this::convertToFolderDTO)
                    .collect(Collectors.toList()));

            content.setDocuments(documentRepository.findRootDocumentsBySpaceId(spaceId)
                    .stream()
                    .map(this::convertToDocumentDTO)
                    .collect(Collectors.toList()));
        }

        content.setBreadcrumbs(breadcrumbs);
        return content;
    }

    // DTO converter
    private DocumentSpaceDTO convertToDTO(DocumentSpace space) {
        DocumentSpaceDTO dto = new DocumentSpaceDTO();
        dto.setId(space.getId());
        dto.setName(space.getName());
        dto.setDescription(space.getDescription());
        dto.setCreatedBy(space.getCreatedBy() != null ? space.getCreatedBy().getUsername() : null);
        dto.setCreatedAt(space.getCreatedAt());
        dto.setUpdatedAt(space.getUpdatedAt());
        return dto;
    }

    private DocumentFolderDTO convertToFolderDTO(DocumentFolder folder) {
        DocumentFolderDTO dto = new DocumentFolderDTO();
        dto.setId(folder.getId());
        dto.setName(folder.getName());
        dto.setDescription(folder.getDescription());
        dto.setParentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getId() : null);
        dto.setDocumentSpaceId(folder.getDocumentSpace() != null ? folder.getDocumentSpace().getId() : null);
        dto.setCreatedBy(folder.getCreatedBy() != null ? folder.getCreatedBy().getUsername() : null);
        dto.setCreatedAt(folder.getCreatedAt());
        dto.setUpdatedAt(folder.getUpdatedAt());
        dto.setDocumentsCount(documentRepository.countDocumentsByFolderId(folder.getId()));
        dto.setSubFoldersCount(folder.getSubFolders() != null ? folder.getSubFolders().size() : 0);
        return dto;
    }

    private DocumentDTO convertToDocumentDTO(Document document) {
        DocumentDTO dto = new DocumentDTO();
        dto.setId(document.getId());
        dto.setName(document.getName());
        dto.setOriginalName(document.getOriginalName());
        dto.setFilePath(document.getFilePath());
        dto.setFileSize(document.getFileSize());
        dto.setMimeType(document.getMimeType());
        dto.setDocumentType(document.getDocumentType());
        dto.setDescription(document.getDescription());
        dto.setFolderId(document.getFolder() != null ? document.getFolder().getId() : null);
        dto.setDocumentSpaceId(document.getDocumentSpace() != null ? document.getDocumentSpace().getId() : null);
        dto.setUploadedBy(document.getUploadedBy() != null ? document.getUploadedBy().getUsername() : null);
        dto.setUploadedAt(document.getUploadedAt());
        dto.setUpdatedAt(document.getUpdatedAt());
        dto.setDownloadUrl("/api/documents/" + document.getId() + "/download");
        return dto;
    }

    private void buildBreadcrumbs(DocumentFolder folder, List<BreadcrumbDTO> breadcrumbs) {
        if (folder.getParentFolder() != null) {
            buildBreadcrumbs(folder.getParentFolder(), breadcrumbs);
        }
        breadcrumbs.add(new BreadcrumbDTO(folder.getId(), folder.getName(), "folder"));
    }
}

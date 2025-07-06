package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.CreateFolderRequest;
import com.example.intranet_back_stage.dto.DocumentDTO;
import com.example.intranet_back_stage.dto.DocumentFolderDTO;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentFolderService {

    private final DocumentFolderRepository folderRepository;
    private final DocumentSpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;

    public DocumentFolderDTO createFolder(CreateFolderRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        DocumentSpace space = spaceRepository.findById(request.getDocumentSpaceId())
                .orElseThrow(() -> new RuntimeException("Document space not found"));

        boolean exists;
        if (request.getParentFolderId() != null) {
            exists = folderRepository.existsByNameAndDocumentSpaceIdAndParentFolderId(
                    request.getName(), request.getDocumentSpaceId(), request.getParentFolderId());
        } else {
            exists = folderRepository.existsByNameAndDocumentSpaceIdAndParentFolderIsNull(
                    request.getName(), request.getDocumentSpaceId());
        }

        if (exists) {
            throw new RuntimeException("Folder already exists in this location");
        }

        DocumentFolder folder = new DocumentFolder();
        folder.setName(request.getName());
        folder.setDescription(request.getDescription());
        folder.setDocumentSpace(space);
        folder.setCreatedBy(user);

        if (request.getParentFolderId() != null) {
            DocumentFolder parent = folderRepository.findById(request.getParentFolderId())
                    .orElseThrow(() -> new RuntimeException("Parent folder not found"));
            folder.setParentFolder(parent);
        }

        return convertToDTO(folderRepository.save(folder));
    }

    public DocumentFolderDTO getFolderById(Long folderId) {
        DocumentFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        return convertToDTO(folder);
    }

    public List<DocumentFolderDTO> getFoldersBySpace(Long spaceId) {
        return folderRepository.findByDocumentSpaceId(spaceId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<DocumentFolderDTO> getSubFolders(Long parentId) {
        return folderRepository.findByParentFolderId(parentId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public void deleteFolder(Long folderId) {
        DocumentFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        folderRepository.delete(folder);
    }

    public DocumentFolderDTO updateFolder(Long folderId, String name, String description) {
        DocumentFolder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        folder.setName(name);
        folder.setDescription(description);
        return convertToDTO(folderRepository.save(folder));
    }

    private DocumentFolderDTO convertToDTO(DocumentFolder folder) {
        DocumentFolderDTO dto = new DocumentFolderDTO();
        dto.setId(folder.getId());
        dto.setName(folder.getName());
        dto.setDescription(folder.getDescription());
        dto.setParentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getId() : null);
        dto.setDocumentSpaceId(folder.getDocumentSpace().getId());
        dto.setCreatedBy(folder.getCreatedBy() != null ? folder.getCreatedBy().getUsername() : null);
        dto.setCreatedAt(folder.getCreatedAt());
        dto.setUpdatedAt(folder.getUpdatedAt());
        dto.setSubFoldersCount(folder.getSubFolders() != null ? folder.getSubFolders().size() : 0);
        dto.setDocumentsCount(folder.getDocuments() != null ? folder.getDocuments().size() : 0);
        return dto;
    }
}
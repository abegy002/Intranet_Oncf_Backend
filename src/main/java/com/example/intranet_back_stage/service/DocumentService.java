package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.DocumentDTO;
import com.example.intranet_back_stage.enums.DocumentType;
import com.example.intranet_back_stage.model.Document;
import com.example.intranet_back_stage.model.DocumentFolder;
import com.example.intranet_back_stage.model.DocumentSpace;
import com.example.intranet_back_stage.model.User;
import com.example.intranet_back_stage.repository.DocumentFolderRepository;
import com.example.intranet_back_stage.repository.DocumentRepository;
import com.example.intranet_back_stage.repository.DocumentSpaceRepository;
import com.example.intranet_back_stage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentSpaceRepository spaceRepository;
    private final DocumentFolderRepository folderRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public DocumentDTO uploadDocument(MultipartFile file, Long spaceId, Long folderId,
                                      String description, Long userId) throws IOException {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        DocumentSpace space = spaceRepository.findById(spaceId).orElseThrow(() -> new RuntimeException("Space not found"));

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String uniqueFilename = UUID.randomUUID() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFilename);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        Document document = new Document();
        document.setName(originalFilename);
        document.setOriginalName(originalFilename);
        document.setFilePath(filePath.toString());
        document.setFileSize(file.getSize());
        document.setMimeType(file.getContentType());
        document.setDocumentType(DocumentType.fromMimeType(file.getContentType()));
        document.setDescription(description);
        document.setDocumentSpace(space);
        document.setUploadedBy(user);

        if (folderId != null) {
            DocumentFolder folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
            document.setFolder(folder);
        }

        return convertToDTO(documentRepository.save(document));
    }

    public Resource downloadDocument(Long documentId) throws IOException {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        Path filePath = Paths.get(doc.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists() || !resource.isReadable())
            throw new RuntimeException("File not readable");
        return resource;
    }

    public void deleteDocument(Long documentId) throws IOException {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        Path filePath = Paths.get(doc.getFilePath());
        if (Files.exists(filePath)) Files.delete(filePath);
        documentRepository.delete(doc);
    }

    public DocumentDTO getDocumentById(Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        return convertToDTO(doc);
    }

    public List<DocumentDTO> getDocumentsByFolder(Long folderId) {
        return documentRepository.findByFolderId(folderId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<DocumentDTO> getDocumentsBySpace(Long spaceId) {
        return documentRepository.findByDocumentSpaceId(spaceId)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public List<DocumentDTO> searchByName(String keyword) {
        return documentRepository.findByNameContaining(keyword)
                .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private DocumentDTO convertToDTO(Document document) {
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
        dto.setDownloadUrl("/documents/" + document.getId() + "/download");
        return dto;
    }
}

// DocumentController.java
package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.dto.DocumentDTO;
import com.example.intranet_back_stage.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public DocumentDTO uploadDocument(@RequestParam("file") MultipartFile file,
                                      @RequestParam Long spaceId,
                                      @RequestParam(required = false) Long folderId,
                                      @RequestParam(required = false) String description,
                                      @RequestParam Long userId) throws IOException {
        return documentService.uploadDocument(file, spaceId, folderId, description, userId);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) throws IOException {
        Resource resource = documentService.downloadDocument(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + resource.getFilename())
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) throws IOException {
        documentService.deleteDocument(id);
    }

    @GetMapping("/{id}")
    public DocumentDTO get(@PathVariable Long id) {
        return documentService.getDocumentById(id);
    }

    @GetMapping("/folder/{folderId}")
    public List<DocumentDTO> getByFolder(@PathVariable Long folderId) {
        return documentService.getDocumentsByFolder(folderId);
    }

    @GetMapping("/space/{spaceId}")
    public List<DocumentDTO> getBySpace(@PathVariable Long spaceId) {
        return documentService.getDocumentsBySpace(spaceId);
    }

    @GetMapping("/search")
    public List<DocumentDTO> search(@RequestParam String keyword) {
        return documentService.searchByName(keyword);
    }
}
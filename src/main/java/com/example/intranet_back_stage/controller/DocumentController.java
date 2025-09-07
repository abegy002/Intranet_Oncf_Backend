// src/main/java/.../controller/DocumentController.java
package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.dto.*;
import com.example.intranet_back_stage.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URL;
import java.time.Duration;

@RestController
@RequestMapping("/docs")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','DOCS_UPLOADER')")
    public DocumentDto create(@RequestPart("metadata") DocumentCreateDto meta,
                              @RequestPart("file") MultipartFile file,
                              Authentication auth) throws Exception {
        return service.create(meta, file, auth.getName());
    }

    @PostMapping(value="/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','DOCS_UPLOADER')")
    public VersionDto addVersion(@PathVariable Long id,
                                 @RequestPart("file") MultipartFile file,
                                 @RequestParam String versionNo,
                                 @RequestParam(required=false) String comment,
                                 Authentication auth) throws Exception {
        return service.addVersion(id, file, versionNo, auth.getName(), comment);
    }

    // === Review actions ===
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public DocumentDto approve(@PathVariable Long id, Authentication auth) {
        return service.approve(id, auth.getName());
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public DocumentDto reject(@PathVariable Long id, @RequestBody DocumentRejectDto dto, Authentication auth) {
        return service.reject(id, dto, auth.getName());
    }

    @PatchMapping("/{id}/abrogate")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public DocumentDto abrogate(@PathVariable Long id, Authentication auth) {
        return service.abrogate(id, auth.getName());
    }

    // === Listings - sensitivity aware ===
    @GetMapping
    public Page<DocumentDto> getAll(@RequestParam(defaultValue="0") int page,
                                    @RequestParam(defaultValue="10") int size,
                                    Authentication auth) {
        return service.getAllVisible(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,"createdAt")), auth);
    }

    @GetMapping("/by-folder")
    public Page<DocumentDto> byFolder(@RequestParam(required=false) Long folderId,
                                      @RequestParam(defaultValue="false") boolean root,
                                      @RequestParam(defaultValue="") String q,
                                      @RequestParam(defaultValue="0") int page,
                                      @RequestParam(defaultValue="10") int size,
                                      Authentication auth) {
        Long effectiveFolderId = root ? null : folderId;
        return service.getByFolderVisible(effectiveFolderId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,"createdAt")), q, auth);
    }

    @GetMapping("/versions/{versionId}/download")
    public ResponseEntity<Void> download(@PathVariable Long versionId, Authentication auth) throws Exception {
        String actor = (auth != null ? auth.getName() : "anonymous");
        URL url = service.downloadUrl(versionId, Duration.ofMinutes(5), actor);
        return ResponseEntity.status(302).location(URI.create(url.toString())).build();
    }

    @GetMapping("/search")
    public Page<DocumentDto> search(@RequestParam(defaultValue="") String q,
                                    @RequestParam(defaultValue="0") int page,
                                    @RequestParam(defaultValue="10") int size,
                                    Authentication auth) {
        return service.search(q, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")), auth);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        service.delete(id, auth != null ? auth.getName() : "anonymous");
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/versions/{versionId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> deleteVersion(@PathVariable Long versionId, Authentication auth) {
        service.deleteVersion(versionId, auth != null ? auth.getName() : "anonymous");
        return ResponseEntity.noContent().build();
    }

}

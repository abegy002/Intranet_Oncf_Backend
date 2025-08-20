package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.dto.DocumentCreateDto;
import com.example.intranet_back_stage.dto.DocumentDto;
import com.example.intranet_back_stage.dto.VersionDto;
import com.example.intranet_back_stage.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.URL;
import java.security.Principal;
import java.time.Duration;

@RestController
@RequestMapping("/docs")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public DocumentDto create(
            @RequestPart("metadata") DocumentCreateDto meta,
            @RequestPart("file") MultipartFile file,
            Principal principal
    ) throws Exception {
        return service.create(meta, file, principal.getName());
    }

    @PostMapping(value="/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public VersionDto addVersion(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam String versionNo,
            @RequestParam(required=false) String comment,
            Principal principal
    ) throws Exception {
        return service.addVersion(id, file, versionNo, principal.getName(), comment);
    }

    @GetMapping
    public Page<DocumentDto> getAll(@RequestParam(defaultValue="0") int page,
                                    @RequestParam(defaultValue="10") int size) {
        return service.getAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,"createdAt")));
    }

    /**
     * Supports:
     * - /docs/by-folder?root=true
     * - /docs/by-folder?folderId=123
     * Optional: &q=search
     */
    @GetMapping("/by-folder")
    public Page<DocumentDto> byFolder(@RequestParam(required=false) Long folderId,
                                      @RequestParam(defaultValue="false") boolean root,
                                      @RequestParam(defaultValue="") String q,
                                      @RequestParam(defaultValue="0") int page,
                                      @RequestParam(defaultValue="10") int size) {
        Long effectiveFolderId = root ? null : folderId;
        return service.getByFolder(effectiveFolderId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC,"createdAt")), q);
    }

    @GetMapping("/{id}")
    public DocumentDto getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/search")
    public Page<DocumentDto> search(@RequestParam(defaultValue="") String q,
                                    @RequestParam(defaultValue="0") int page,
                                    @RequestParam(defaultValue="10") int size) {
        return service.search(q, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @GetMapping("/versions/{versionId}/download")
    public ResponseEntity<Void> download(
            @PathVariable Long versionId,
            java.security.Principal p
    ) throws Exception {
        // null-safe actor for audit trail
        String actor = (p != null && p.getName() != null && !p.getName().isBlank())
                ? p.getName()
                : "anonymous";

        URL url = service.downloadUrl(versionId, Duration.ofMinutes(5), actor);
        return ResponseEntity.status(302).location(URI.create(url.toString())).build();
    }

    // DocumentController.java
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> delete(@PathVariable Long id, java.security.Principal p) {
        String actor = (p != null && p.getName() != null && !p.getName().isBlank())
                ? p.getName()
                : "anonymous";
        service.delete(id, actor);
        return ResponseEntity.noContent().build();
    }

}

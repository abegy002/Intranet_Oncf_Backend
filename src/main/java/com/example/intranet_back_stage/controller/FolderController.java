package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.dto.*;
import com.example.intranet_back_stage.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService service;

    @GetMapping("/root")
    public List<FolderDto> root() {
        return service.listRoot();
    }

    @GetMapping("/{id}/children")
    public List<FolderDto> children(@PathVariable Long id) {
        return service.listChildren(id);
    }

    @GetMapping("/tree")
    public List<FolderNodeDto> tree() {
        return service.tree();
    }

    @GetMapping("/{id}")
    public FolderDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','DOCS_EDITOR')")
    public FolderDto create(@RequestBody FolderCreateDto dto) {
        return service.create(dto);
    }

    /** JSON body: { "name": "New Name" } */
    @PatchMapping("/{id}/rename")
    @PreAuthorize("hasAnyRole('ADMIN','HR','DOCS_EDITOR')")
    public FolderDto renamePatch(@PathVariable Long id, @RequestBody FolderRenameDto dto) {
        return service.rename(id, dto.name());
    }

    /** text/plain body with the new name (to support PUT from your Angular) */
    @PutMapping(value = "/{id}/rename", consumes = "text/plain")
    @PreAuthorize("hasAnyRole('ADMIN','HR','DOCS_EDITOR')")
    public FolderDto renamePut(@PathVariable Long id, @RequestBody byte[] raw) {
        String newName = new String(raw, StandardCharsets.UTF_8);
        return service.rename(id, newName);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','DOCS_EDITOR')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}

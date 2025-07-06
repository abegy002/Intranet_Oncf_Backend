// DocumentFolderController.java
package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.dto.CreateFolderRequest;
import com.example.intranet_back_stage.dto.DocumentFolderDTO;
import com.example.intranet_back_stage.service.DocumentFolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
public class DocumentFolderController {

    private final DocumentFolderService folderService;

    @PostMapping
    public DocumentFolderDTO create(@RequestBody CreateFolderRequest request,
                                    @RequestParam Long userId) {
        return folderService.createFolder(request, userId);
    }

    @GetMapping("/{id}")
    public DocumentFolderDTO get(@PathVariable Long id) {
        return folderService.getFolderById(id);
    }

    @GetMapping("/space/{spaceId}")
    public List<DocumentFolderDTO> getBySpace(@PathVariable Long spaceId) {
        return folderService.getFoldersBySpace(spaceId);
    }

    @GetMapping("/subfolders/{parentId}")
    public List<DocumentFolderDTO> getSubFolders(@PathVariable Long parentId) {
        return folderService.getSubFolders(parentId);
    }

    @PutMapping("/{id}")
    public DocumentFolderDTO update(@PathVariable Long id,
                                    @RequestParam String name,
                                    @RequestParam String description) {
        return folderService.updateFolder(id, name, description);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        folderService.deleteFolder(id);
    }
}

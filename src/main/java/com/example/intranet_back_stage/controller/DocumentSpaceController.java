// DocumentSpaceController.java
package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.dto.CreateSpaceRequest;
import com.example.intranet_back_stage.dto.DocumentSpaceContentDTO;
import com.example.intranet_back_stage.dto.DocumentSpaceDTO;
import com.example.intranet_back_stage.service.DocumentSpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/spaces")
@RequiredArgsConstructor
public class DocumentSpaceController {

    private final DocumentSpaceService documentSpaceService;

    @PostMapping
    public DocumentSpaceDTO create(@RequestBody CreateSpaceRequest request) {
        return documentSpaceService.createDocumentSpace(
                request.getName(),
                request.getDescription(),
                request.getUserId()
        );
    }

    @GetMapping
    public List<DocumentSpaceDTO> getAll() {
        return documentSpaceService.getAllDocumentSpaces();
    }

    @GetMapping("/{id}")
    public DocumentSpaceDTO getById(@PathVariable Long id) {
        return documentSpaceService.getDocumentSpaceById(id);
    }

    @PutMapping("/{id}")
    public DocumentSpaceDTO update(@PathVariable Long id,
                                   @RequestParam String name,
                                   @RequestParam String description) {
        return documentSpaceService.updateDocumentSpace(id, name, description);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        documentSpaceService.deleteDocumentSpace(id);
    }

    @GetMapping("/{spaceId}/content")
    public DocumentSpaceContentDTO getContent(@PathVariable Long spaceId,
                                              @RequestParam(required = false) Long folderId) {
        return documentSpaceService.getSpaceContent(spaceId, folderId);
    }
}
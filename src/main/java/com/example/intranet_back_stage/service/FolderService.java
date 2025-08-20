// service/FolderService.java
package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.*;
import com.example.intranet_back_stage.model.Folder;
import com.example.intranet_back_stage.repository.DocumentRepository;
import com.example.intranet_back_stage.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Service @RequiredArgsConstructor
public class FolderService {
    private final FolderRepository folderRepo;
    private final DocumentRepository docRepo;

    public List<FolderDto> listRoot() {
        return folderRepo.findByParentIsNullOrderByNameAsc()
                .stream().map(this::toDto).toList();
    }

    public List<FolderDto> listChildren(Long parentId) {
        return folderRepo.findByParentIdOrderByNameAsc(parentId)
                .stream().map(this::toDto).toList();
    }

    public FolderDto get(Long id) {
        return folderRepo.findById(id).map(this::toDto)
                .orElseThrow();
    }

    @Transactional
    public FolderDto create(FolderCreateDto dto) {
        Folder parent = (dto.parentId() != null) ? folderRepo.findById(dto.parentId()).orElseThrow() : null;
        String path = computePath(parent, dto.name());
        if (folderRepo.findByPath(path).isPresent()) {
            throw new IllegalStateException("Folder already exists at: " + path);
        }
        Folder f = new Folder();
        f.setName(dto.name().trim());
        f.setParent(parent);
        f.setPath(path);
        f = folderRepo.save(f);
        return toDto(f);
    }

    @Transactional
    public FolderDto rename(Long id, String newName) {
        Folder f = folderRepo.findById(id).orElseThrow();
        String oldPath = f.getPath();
        String newPath = computePath(f.getParent(), newName);
        if (!oldPath.equals(newPath) && folderRepo.findByPath(newPath).isPresent()) {
            throw new IllegalStateException("Folder already exists at: " + newPath);
        }
        f.setName(newName.trim());
        f.setPath(newPath);
        folderRepo.save(f);

        // update subtree paths
        List<Folder> children = folderRepo.findByParentIdOrderByNameAsc(f.getId());
        for (Folder c : children) {
            updateSubtreePath(c, oldPath, newPath);
        }
        return toDto(f);
    }

    @Transactional
    public void delete(Long id) {
        if (folderRepo.existsByParentId(id)) {
            throw new IllegalStateException("Folder has child folders");
        }
        long docs = docRepo.countByFolderId(id);
        if (docs > 0) {
            throw new IllegalStateException("Folder contains documents");
        }
        folderRepo.deleteById(id);
    }

    public List<FolderNodeDto> tree() {
        List<Folder> roots = folderRepo.findByParentIsNullOrderByNameAsc();
        List<FolderNodeDto> out = new ArrayList<>();
        for (Folder r : roots) out.add(toNode(r));
        return out;
    }

    /* helpers */

    private void updateSubtreePath(Folder node, String oldPrefix, String newPrefix) {
        node.setPath(node.getPath().replaceFirst("^" + java.util.regex.Pattern.quote(oldPrefix), newPrefix));
        folderRepo.save(node);
        for (Folder ch : folderRepo.findByParentIdOrderByNameAsc(node.getId())) {
            updateSubtreePath(ch, oldPrefix, newPrefix);
        }
    }

    private String computePath(Folder parent, String name) {
        String safe = slug(name);
        if (parent == null) return "/" + safe;
        return parent.getPath().equals("/") ? "/" + safe : parent.getPath() + "/" + safe;
    }

    private String slug(String s) {
        String n = Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+","");
        n = n.replaceAll("[^a-zA-Z0-9-_ ]","").replace(' ','-');
        return n.isBlank() ? "folder" : n;
    }

    private FolderDto toDto(Folder f) {
        int childrenCount = folderRepo.findByParentIdOrderByNameAsc(f.getId()).size();
        return new FolderDto(
                f.getId(), f.getName(), f.getPath(),
                f.getParent() != null ? f.getParent().getId() : null,
                f.getCreatedAt(), childrenCount);
    }

    private FolderNodeDto toNode(Folder f) {
        List<Folder> children = folderRepo.findByParentIdOrderByNameAsc(f.getId());
        return new FolderNodeDto(
                f.getId(), f.getName(), f.getPath(),
                children.stream().map(this::toNode).toList()
        );
    }
}

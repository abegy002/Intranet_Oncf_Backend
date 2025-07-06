package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.CreateNewsRequest;
import com.example.intranet_back_stage.dto.NewsResponseDTO;
import com.example.intranet_back_stage.model.News;
import com.example.intranet_back_stage.model.User;
import com.example.intranet_back_stage.repository.NewsRepository;
import com.example.intranet_back_stage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NewsService {
    private final NewsRepository newsRepo;
    private final UserRepository userRepo;
    private final Path uploadDir = Paths.get("uploads", "news-images");

    private NewsResponseDTO toDto(News n) {
        return NewsResponseDTO.builder()
                .id(n.getId())
                .title(n.getTitle())
                .imagePath(n.getImagePath())
                .description(n.getDescription())
                .content(n.getContent())
                .ownerUsername(n.getOwner().getUsername())
                .createdAt(n.getCreatedAt())
                .updatedAt(n.getUpdatedAt())
                .build();
    }

    public List<NewsResponseDTO> findAll() {
        return newsRepo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public NewsResponseDTO findById(Long id) {
        return toDto(newsRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found")));
    }

    public NewsResponseDTO create(CreateNewsRequest req) throws IOException {
        if (!Files.exists(uploadDir)) Files.createDirectories(uploadDir);

        String filename = System.currentTimeMillis() + "-" + req.getImage().getOriginalFilename();
        Path target = uploadDir.resolve(filename);
        Files.copy(req.getImage().getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        User owner = userRepo.findById(req.getOwnerId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        News n = News.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .content(req.getContent())
                .owner(owner)
                .imagePath("/uploads/news-images/" + filename)
                .build();

        return toDto(newsRepo.save(n));
    }

    public NewsResponseDTO update(Long id, String title, String description, String content) {
        News n = newsRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found"));
        n.setTitle(title);
        n.setDescription(description);
        n.setContent(content);
        return toDto(newsRepo.save(n));
    }

    public void delete(Long id) {
        newsRepo.delete(newsRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("News not found")));
    }
}

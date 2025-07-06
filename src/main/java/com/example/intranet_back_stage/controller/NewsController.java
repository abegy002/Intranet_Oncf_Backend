package com.example.intranet_back_stage.controller;


import com.example.intranet_back_stage.dto.CreateNewsRequest;
import com.example.intranet_back_stage.dto.NewsResponseDTO;
import com.example.intranet_back_stage.dto.UpdateNewsRequest;
import com.example.intranet_back_stage.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsController {
    private final NewsService newsService;

    @GetMapping
    public List<NewsResponseDTO> list() {
        return newsService.findAll();
    }

    @GetMapping("/{id}")
    public NewsResponseDTO get(@PathVariable Long id) {
        return newsService.findById(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public NewsResponseDTO create(@ModelAttribute CreateNewsRequest req) throws IOException {
        return newsService.create(req);
    }

    @PutMapping("/{id}")
    public NewsResponseDTO update( @PathVariable Long id, @RequestBody UpdateNewsRequest req ) {
        return newsService.update(id, req.title, req.description, req.content);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        newsService.delete(id);
    }
}


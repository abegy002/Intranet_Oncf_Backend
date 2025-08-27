// src/main/java/.../controller/EventController.java
package com.example.intranet_back_stage.controller;

import com.example.intranet_back_stage.dto.EventCreateDto;
import com.example.intranet_back_stage.dto.EventDto;
import com.example.intranet_back_stage.dto.EventUpdateDto;
import com.example.intranet_back_stage.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService service;

    @GetMapping
    public org.springframework.data.domain.Page<EventDto> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return service.listAll(page, size);
    }

    // Create WITH image (multipart) — matches your Angular FormData(data + file)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public EventDto createMultipart(@RequestPart("data") EventCreateDto data,
                                    @RequestPart(value = "file", required = false) MultipartFile file,
                                    Principal principal) throws IOException {
        return service.createWithCover(data, file, principal.getName());
    }

    // Optional: plain JSON create
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public EventDto createJson(@RequestBody EventCreateDto data, Principal principal) throws IOException {
        return service.createWithCover(data, null, principal.getName());
    }

    // Update (JSON)
    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public EventDto update(@PathVariable Long id, @RequestBody EventUpdateDto dto, Principal p) {
        return service.update(id, dto, p.getName());
    }

    // Replace/upload cover for an existing event (multipart with "file")
    @PostMapping(path = "/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public EventDto uploadCover(@PathVariable Long id, @RequestPart("file") MultipartFile file) throws IOException {
        return service.uploadCover(id, file);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/{id}")
    public EventDto get(@PathVariable Long id) { return service.get(id); }

    @GetMapping("/upcoming")
    public org.springframework.data.domain.Page<EventDto> upcoming(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "12") int size,
                                                                   Authentication auth) {
        boolean includeInternal = auth != null && auth.isAuthenticated() &&
                auth.getAuthorities().stream().anyMatch(a ->
                        "ROLE_USER".equals(a.getAuthority()) ||
                                "ROLE_ADMIN".equals(a.getAuthority()) ||
                                "ROLE_HR".equals(a.getAuthority())
                );
        return service.upcoming(page, size, includeInternal);
    }

    // inside @RestController
    @GetMapping("/{id}/cover-url")
    public Map<String, String> coverUrl(@PathVariable Long id,
                                        @RequestParam(required = false) Long ttlSeconds) throws Exception {
        String url = service.coverSignedUrl(id, ttlSeconds != null ? Duration.ofSeconds(ttlSeconds) : null);
        return Map.of("url", url);
    }
}



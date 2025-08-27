package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.EventCreateDto;
import com.example.intranet_back_stage.dto.EventDto;
import com.example.intranet_back_stage.dto.EventUpdateDto;
import com.example.intranet_back_stage.model.Event;
import com.example.intranet_back_stage.repository.EventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.*;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository repo;
    private final StorageService storage;

    private static final ZoneId SERVER_ZONE = ZoneId.systemDefault();

    /* ====== Create ====== */
    @Transactional
    public EventDto createWithCover(EventCreateDto dto, MultipartFile file, String username) {
        validateDates(dto.startsAt(), dto.endsAt());
        if (dto.visibility() != null) validateVisibility(dto.visibility());
        validateCapacity(dto.capacity());

        Event e = new Event();
        e.setTitle(dto.title());
        e.setDescription(dto.description());
        e.setLocation(dto.location());
        e.setStartsAt(toLocal(dto.startsAt()));
        e.setEndsAt(dto.endsAt() != null ? toLocal(dto.endsAt()) : null);
        e.setCapacity(dto.capacity());
        e.setVisibility(defaultIfBlank(dto.visibility(), "INTERNAL"));
        e.setStatus("PLANNED"); // default; may be auto-ended below
        e.setCreatedBy(username);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(null);

        // If the event already ended by the time we create it, mark ENDED
        applyAutoEnded(e);

        e = repo.save(e);

        if (file != null && !file.isEmpty()) {
            storeCoverFile(e, file);
            e = repo.save(e);
        }
        return toDto(e);
    }

    /* ====== Update ====== */
    @Transactional
    public EventDto update(Long id, EventUpdateDto dto, String username) {
        Event e = repo.findById(id).orElseThrow();

        // Dates (if any) validation
        OffsetDateTime start = dto.startsAt() != null ? dto.startsAt()
                : e.getStartsAt() != null ? e.getStartsAt().atZone(SERVER_ZONE).toOffsetDateTime() : null;
        OffsetDateTime end = dto.endsAt() != null ? dto.endsAt()
                : e.getEndsAt() != null ? e.getEndsAt().atZone(SERVER_ZONE).toOffsetDateTime() : null;
        if (start != null) validateDates(start, end);

        // Validate controlled fields
        if (dto.visibility() != null) validateVisibility(dto.visibility());
        if (dto.status() != null) validateStatus(dto.status());
        if (dto.capacity() != null) validateCapacity(dto.capacity());

        if (dto.title() != null) e.setTitle(dto.title());
        if (dto.description() != null) e.setDescription(dto.description());
        if (dto.location() != null) e.setLocation(dto.location());
        if (dto.startsAt() != null) e.setStartsAt(toLocal(dto.startsAt()));
        if (dto.endsAt() != null) e.setEndsAt(toLocal(dto.endsAt()));
        if (dto.capacity() != null) e.setCapacity(dto.capacity());
        if (dto.visibility() != null) e.setVisibility(dto.visibility());
        if (dto.status() != null) e.setStatus(dto.status());

        // If end has passed (and not CANCELLED), flip to ENDED automatically
        applyAutoEnded(e);

        e.setUpdatedAt(LocalDateTime.now());
        return toDto(repo.save(e));
    }

    @Transactional
    public void delete(Long id) {
        Event e = repo.findById(id).orElseThrow();
        deleteOldCoverIfExists(e.getCoverKey());
        repo.delete(e);
    }

    @Transactional
    public EventDto uploadCover(Long id, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Le fichier est requis.");
        String ct = Objects.toString(file.getContentType(), "").toLowerCase();
        if (!ct.startsWith("image/")) throw new IllegalArgumentException("Seules les images sont acceptées.");

        Event e = repo.findById(id).orElseThrow();
        deleteOldCoverIfExists(e.getCoverKey());
        storeCoverFile(e, file);
        e.setUpdatedAt(LocalDateTime.now());
        return toDto(repo.save(e));
    }

    /* ====== Cover signed URL helper ====== */
    public String coverSignedUrl(Long id, Duration ttl) throws Exception {
        Event e = repo.findById(id).orElseThrow();
        if (e.getCoverKey() == null || e.getCoverKey().isBlank()) return null;
        URL url = storage.getSignedUrl(e.getCoverKey(), ttl);
        return url.toString();
    }

    /* ====== Queries ====== */

    // includeInternal controls visibility filtering; upcoming excludes CANCELLED and (by 'startsAtAfter') anything already started
    public Page<EventDto> upcoming(int page, int size, boolean includeInternal) {
        Pageable p = PageRequest.of(page, size);
        var vis = includeInternal
                ? java.util.List.of("PUBLIC", "INTERNAL")
                : java.util.List.of("PUBLIC");

        return repo
                .findByStartsAtAfterAndStatusNotAndVisibilityInOrderByStartsAtAsc(LocalDateTime.now(), "CANCELLED", vis, p)
                .map(this::toDto);
    }

    // keep listAll unfiltered for admin UIs
    public Page<EventDto> listAll(int page, int size) {
        Pageable p = PageRequest.of(page, size);
        return repo.findAll(p).map(this::toDto);
    }

    public EventDto get(Long id) {
        return toDto(repo.findById(id).orElseThrow());
    }

    /* ====== Validation & helpers ====== */

    private void validateDates(OffsetDateTime startsAt, OffsetDateTime endsAt) {
        if (startsAt == null) throw new IllegalArgumentException("startsAt est obligatoire.");
        if (endsAt != null && endsAt.isBefore(startsAt)) {
            throw new IllegalArgumentException("endsAt doit être postérieur à startsAt.");
        }
    }

    private void validateStatus(String status) {
        if (!java.util.Set.of("PLANNED","CANCELLED","POSTPONED","ENDED").contains(status)) {
            throw new IllegalArgumentException("Statut invalide. Attendu: PLANNED|CANCELLED|POSTPONED|ENDED.");
        }
    }

    private void validateVisibility(String vis) {
        if (!java.util.Set.of("INTERNAL","PUBLIC").contains(vis)) {
            throw new IllegalArgumentException("Visibilité invalide. Attendu: INTERNAL|PUBLIC.");
        }
    }

    private void validateCapacity(Integer capacity) {
        if (capacity != null && capacity < 0) {
            throw new IllegalArgumentException("Capacity must be >= 0.");
        }
    }

    private LocalDateTime toLocal(OffsetDateTime odt) {
        return odt.atZoneSameInstant(SERVER_ZONE).toLocalDateTime();
    }

    private String defaultIfBlank(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }

    private String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) return "image";
        String name = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        return name.length() > 120 ? name.substring(name.length() - 120) : name;
    }

    private void storeCoverFile(Event e, MultipartFile file) {
        try {
            String ct = Objects.toString(file.getContentType(), "application/octet-stream");
            if (!ct.toLowerCase().startsWith("image/")) {
                throw new IllegalArgumentException("Seules les images sont acceptées.");
            }
            String safe = sanitizeFilename(file.getOriginalFilename());
            String objectKey = "event-covers/" + e.getId() + "/" + System.currentTimeMillis() + "-" + safe;
            storage.put(file.getInputStream(), file.getSize(), ct, objectKey);
            e.setCoverKey(objectKey);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to store cover image", ex);
        }
    }

    private void deleteOldCoverIfExists(String coverKey) {
        if (coverKey == null || coverKey.isBlank()) return;
        try { storage.delete(coverKey); } catch (Exception ignored) {}
    }

    private String makeSignedCoverUrl(String key) {
        if (key == null || key.isBlank()) return null;
        try {
            URL url = storage.getSignedUrl(key, null);
            return url.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** Apply automatic transition to ENDED if endsAt has passed and current status isn't CANCELLED. */
    private void applyAutoEnded(Event e) {
        if (e == null) return;
        if ("CANCELLED".equals(e.getStatus())) return;
        LocalDateTime end = e.getEndsAt();
        if (end != null && end.isBefore(LocalDateTime.now(SERVER_ZONE))) {
            e.setStatus("ENDED");
        }
    }

    /** Compute the status to expose to clients (ENDed if past end time, unless CANCELLED). */
    private String effectiveStatus(Event e) {
        if ("CANCELLED".equals(e.getStatus())) return "CANCELLED";
        LocalDateTime end = e.getEndsAt();
        if (end != null && end.isBefore(LocalDateTime.now(SERVER_ZONE))) return "ENDED";
        return e.getStatus();
    }

    private EventDto toDto(Event e) {
        boolean hasCover = e.getCoverKey() != null && !e.getCoverKey().isBlank();
        String coverUrl = hasCover ? makeSignedCoverUrl(e.getCoverKey()) : null;

        // Always return the effective (derived) status to callers
        String statusForClient = effectiveStatus(e);

        return new EventDto(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getLocation(),
                e.getStartsAt(),
                e.getEndsAt(),
                statusForClient,
                e.getVisibility(),
                e.getCapacity(),
                hasCover,
                coverUrl,
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}

package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.DocumentCreateDto;
import com.example.intranet_back_stage.dto.DocumentDto;
import com.example.intranet_back_stage.dto.DocumentRejectDto;
import com.example.intranet_back_stage.dto.VersionDto;
import com.example.intranet_back_stage.enums.DocumentStatus;
import com.example.intranet_back_stage.enums.Sensitivity;
import com.example.intranet_back_stage.model.Document;
import com.example.intranet_back_stage.model.DocumentAudit;
import com.example.intranet_back_stage.model.DocumentVersion;
import com.example.intranet_back_stage.model.Folder;
import com.example.intranet_back_stage.repository.DocumentAuditRepository;
import com.example.intranet_back_stage.repository.DocumentRepository;
import com.example.intranet_back_stage.repository.DocumentVersionRepository;
import com.example.intranet_back_stage.repository.FolderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository docRepo;
    private final DocumentVersionRepository verRepo;
    private final DocumentAuditRepository auditRepo;
    private final FolderRepository folderRepo;
    private final StorageService storage;

    private final FileIconService fileIconService;
    private final NotificationService notificationService;

    private static final String TYPE_DOC_CREATED        = "DOC_SUBMITTED";
    private static final String TYPE_DOC_APPROVED       = "DOC_APPROVED";
    private static final String TYPE_DOC_REJECTED       = "DOC_REJECTED";
    private static final String TYPE_DOC_ABROGATED      = "DOC_ABROGATED";
    private static final String TYPE_DOC_NEWVER_REVIEW  = "DOC_NEWVER_REVIEW";

    private static final String ROLE_EMPLOYEE    = "USER";
    private static final String ROLE_DOCS_VIEWER = "DOCS_VIEWER";
    private static final String ROLE_HR          = "HR";
    private static final String ROLE_ADMIN       = "ADMIN";

    /* ===================== CREATE (always IN_REVIEW) ===================== */
    @Transactional
    public DocumentDto create(DocumentCreateDto meta, MultipartFile file, String username) throws Exception {
        Folder folder = (meta.folderId() != null)
                ? folderRepo.findById(meta.folderId()).orElseThrow(() -> new IllegalArgumentException("Folder not found"))
                : null;

        Document d = new Document();
        d.setFolder(folder);
        d.setTitle(meta.title());
        d.setDocType(meta.docType());                       // enum in the entity
        d.setStatus(DocumentStatus.IN_REVIEW);              // submitted for review
        d.setSensitivity(Optional.ofNullable(meta.sensitivity()).orElse(Sensitivity.INTERNAL));
        d.setOwner(username);
        d.setCreatedAt(LocalDateTime.now());
        d = docRepo.save(d);

        addVersionInternal(d, file, "1.0", username, "Initial upload");
        audit("CREATE", d.getId(), username, "Submitted for review");

        // 🔔 notify HR/Admin review queue
        safeNotifyRole(ROLE_HR, TYPE_DOC_CREATED, "Nouveau document à valider",
                d.getTitle(), uiDocListLink(d.getId(), folder), username);
        safeNotifyRole(ROLE_ADMIN, TYPE_DOC_CREATED, "Nouveau document à valider",
                d.getTitle(), uiDocListLink(d.getId(), folder), username);

        return toDto(d);
    }

    /* ===================== REVIEW ACTIONS ===================== */

    @Transactional
    public DocumentDto approve(Long docId, String reviewer) {
        Document d = docRepo.findById(docId).orElseThrow();
        if (d.getStatus() != DocumentStatus.IN_REVIEW) {
            throw new IllegalStateException("Seuls les documents en revue peuvent être approuvés.");
        }
        d.setStatus(DocumentStatus.PUBLISHED);
        d.setReviewedBy(reviewer);
        d.setReviewedAt(LocalDateTime.now());
        d.setRejectionReason(null);
        d.setPublishedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        d = docRepo.save(d);

        audit("APPROVE", d.getId(), reviewer, "Publication");
        safeNotifyRole(ROLE_EMPLOYEE, TYPE_DOC_APPROVED, "Document publié", d.getTitle(),
                uiDocListLink(d.getId(), d.getFolder()), reviewer);

        return toDto(d);
    }

    @Transactional
    public DocumentDto reject(Long docId, DocumentRejectDto dto, String reviewer) {
        Document d = docRepo.findById(docId).orElseThrow();
        if (d.getStatus() != DocumentStatus.IN_REVIEW) {
            throw new IllegalStateException("Seuls les documents en revue peuvent être rejetés.");
        }
        d.setStatus(DocumentStatus.REJECTED);
        d.setReviewedBy(reviewer);
        d.setReviewedAt(LocalDateTime.now());
        d.setRejectionReason(dto != null ? dto.reason() : null);
        d.setUpdatedAt(LocalDateTime.now());
        d = docRepo.save(d);

        audit("REJECT", d.getId(), reviewer, d.getRejectionReason());

        // 🔔 notify owner using username-only overload (ID resolved internally)
        notificationService.createForUser(
                d.getOwner(),
                TYPE_DOC_REJECTED,
                "Votre document a été rejeté",
                Optional.ofNullable(d.getRejectionReason()).orElse("Consultez les commentaires du relecteur."),
                reviewer
        );

        return toDto(d);
    }

    @Transactional
    public DocumentDto abrogate(Long docId, String actor) {
        Document d = docRepo.findById(docId).orElseThrow();
        if (d.getStatus() != DocumentStatus.PUBLISHED) {
            throw new IllegalStateException("Seuls les documents publiés peuvent être abrogés.");
        }
        d.setStatus(DocumentStatus.WITHDRAWN);
        d.setAbrogatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        d = docRepo.save(d);

        audit("ABROGATE", d.getId(), actor, "Retiré de la circulation");
        safeNotifyRole(ROLE_EMPLOYEE, TYPE_DOC_ABROGATED, "Document retiré", d.getTitle(),
                uiDocListLink(d.getId(), d.getFolder()), actor);
        return toDto(d);
    }

    /* ===================== VERSIONING RULE ===================== */
    @Transactional
    public VersionDto addVersion(Long docId, MultipartFile file, String versionNo, String username, String comment) throws Exception {
        Document d = docRepo.findById(docId).orElseThrow();
        VersionDto v = addVersionInternal(d, file, versionNo, username, comment);

        // if the doc was published, new version returns to IN_REVIEW
        if (d.getStatus() == DocumentStatus.PUBLISHED) {
            d.setStatus(DocumentStatus.IN_REVIEW);
            d.setUpdatedAt(LocalDateTime.now());
            d.setReviewedBy(null);
            d.setReviewedAt(null);
            d.setRejectionReason(null);
            docRepo.save(d);

            audit("UPDATE", d.getId(), username, "New version; back to IN_REVIEW");
            safeNotifyRole(ROLE_HR, TYPE_DOC_NEWVER_REVIEW, "Nouvelle version à valider",
                    d.getTitle() + " v" + versionNo, uiDocListLink(d.getId(), d.getFolder()), username);
            safeNotifyRole(ROLE_ADMIN, TYPE_DOC_NEWVER_REVIEW, "Nouvelle version à valider",
                    d.getTitle() + " v" + versionNo, uiDocListLink(d.getId(), d.getFolder()), username);
        } else {
            audit("UPDATE", d.getId(), username, "New version " + versionNo);
        }
        return v;
    }

    /* ===================== Visibility / Sensitivity filtering ===================== */

    /** Returns a page filtered by user’s rights against sensitivity. */
    public Page<DocumentDto> getAllVisible(Pageable p, Authentication auth) {
        Page<Document> page = docRepo.findAll(p);
        List<DocumentDto> visible = page.getContent().stream()
                .filter(d -> canSee(d, auth))
                .map(this::toDto)
                .toList();
        return new PageImpl<>(visible, p, page.getTotalElements());
    }

    public Page<DocumentDto> getByFolderVisible(Long folderId, Pageable p, String q, Authentication auth) {
        Page<Document> page = getByFolderRaw(folderId, p, q);
        List<DocumentDto> visible = page.getContent().stream()
                .filter(d -> canSee(d, auth))
                .map(this::toDto)
                .toList();
        return new PageImpl<>(visible, p, page.getTotalElements());
    }

    private Page<Document> getByFolderRaw(Long folderId, Pageable p, String q) {
        String query = (q == null) ? "" : q.trim();
        Specification<Document> spec = (root, cq, cb) -> {
            var folderExpr = root.get("folder");
            var inFolder = (folderId == null) ? cb.isNull(folderExpr) : cb.equal(folderExpr.get("id"), folderId);
            if (query.isEmpty()) return inFolder;
            var titleLike = cb.like(cb.lower(root.get("title")), "%" + query.toLowerCase() + "%");
            var typeLike  = cb.like(cb.lower(root.get("docType").as(String.class)), "%" + query.toLowerCase() + "%");
            return cb.and(inFolder, cb.or(titleLike, typeLike));
        };
        return docRepo.findAll(spec, p);
    }

    public Page<DocumentDto> search(String q, Pageable p, Authentication auth) {
        String query = (q == null) ? "" : q.trim();
        Specification<Document> spec = (root, cq, cb) -> {
            if (query.isEmpty()) return cb.conjunction();
            var titleLike = cb.like(cb.lower(root.get("title")), "%" + query.toLowerCase() + "%");
            var typeLike  = cb.like(cb.lower(root.get("docType").as(String.class)), "%" + query.toLowerCase() + "%");
            return cb.or(titleLike, typeLike);
        };

        Page<Document> page = docRepo.findAll(spec, p);
        List<DocumentDto> visible = page.getContent().stream()
                .filter(d -> canSee(d, auth))
                .map(this::toDto)
                .toList();

        return new PageImpl<>(visible, p, page.getTotalElements());
    }

    private boolean canSee(Document d, Authentication auth) {
        if (d.getSensitivity() == Sensitivity.INTERNAL) return true;
        if (auth == null) return false;
        boolean isOwner = d.getOwner() != null && d.getOwner().equalsIgnoreCase(auth.getName());
        boolean isAdmin = hasAnyRole(auth, "ADMIN");
        boolean isHr    = hasAnyRole(auth, "HR");
        return isOwner || isAdmin || (d.getSensitivity() == Sensitivity.SENSITIVE && isHr);
    }

    private boolean hasAnyRole(Authentication auth, String role) {
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    /* ===================== Storage & versioning internals ===================== */

    private VersionDto addVersionInternal(Document d, MultipartFile file, String versionNo, String username, String comment) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File required");

        String folderPath   = (d.getFolder() == null) ? "root" : sanitizeFolderPath(d.getFolder().getPath());
        String titleSlug    = slug(d.getTitle());
        String safeOriginal = sanitizeFilename(file.getOriginalFilename());

        String objectKey = String.format("docs/%s/%s/v%s/%s", folderPath, titleSlug, versionNo, safeOriginal);

        String key = storage.put(file.getInputStream(), file.getSize(), file.getContentType(), objectKey);

        DocumentVersion v = new DocumentVersion();
        v.setDocument(d);
        v.setVersionNo(versionNo);
        v.setFilename(file.getOriginalFilename());
        v.setStorageKey(key);
        v.setSize(file.getSize());
        v.setCreatedBy(username);
        v.setCreatedAt(LocalDateTime.now());
        v.setComment(comment);
        verRepo.save(v);

        d.setUpdatedAt(LocalDateTime.now());
        docRepo.save(d);

        return new VersionDto(v.getId(), v.getVersionNo(), v.getFilename(), v.getSize(), v.getCreatedBy(), v.getCreatedAt());
    }

    public URL downloadUrl(Long versionId, Duration ttl, String username) throws Exception {
        DocumentVersion v = verRepo.findById(versionId).orElseThrow();
        audit("DOWNLOAD", v.getDocument().getId(), username, "v=" + v.getVersionNo());
        return storage.getSignedUrl(v.getStorageKey(), ttl);
    }

    @Transactional
    public void delete(Long docId, String username) {
        Document d = docRepo.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));

        List<DocumentVersion> versions = verRepo.findByDocumentIdOrderByCreatedAtDesc(docId);

        for (DocumentVersion v : versions) {
            try {
                if (v.getStorageKey() != null && !v.getStorageKey().isBlank()) {
                    storage.delete(v.getStorageKey());
                }
            } catch (Exception ex) {
                System.err.println("Failed to delete blob: " + v.getStorageKey() + " -> " + ex.getMessage());
            }
        }

        verRepo.deleteAll(versions);
        docRepo.delete(d);
        audit("DELETE", docId, username, "Document and " + versions.size() + " version(s) removed");
    }

    @Transactional
    public void deleteVersion(Long versionId, String username) {
        DocumentVersion v = verRepo.findById(versionId).orElseThrow();
        Document d = v.getDocument();

        try {
            if (v.getStorageKey() != null && !v.getStorageKey().isBlank()) {
                storage.delete(v.getStorageKey());
            }
        } catch (Exception ex) {
            System.err.println("Failed to delete blob: " + v.getStorageKey() + " -> " + ex.getMessage());
        }

        verRepo.delete(v);

        d.setUpdatedAt(LocalDateTime.now());
        docRepo.save(d);

        audit("DELETE_VERSION", d.getId(), username, "versionId=" + versionId + ", v=" + v.getVersionNo());
    }

    private DocumentDto toDto(Document d) {
        var versions = verRepo.findByDocumentIdOrderByCreatedAtDesc(d.getId()).stream()
                .map(v -> new VersionDto(v.getId(), v.getVersionNo(), v.getFilename(), v.getSize(), v.getCreatedBy(), v.getCreatedAt()))
                .toList();

        String latestFilename = versions.isEmpty() ? null : versions.get(0).filename();
        String docTypeKey = (d.getDocType() != null) ? d.getDocType().name() : null;
        String iconUrl = fileIconService.iconFor(docTypeKey, latestFilename);

        return new DocumentDto(
                d.getId(),
                d.getTitle(),
                d.getDocType(),
                d.getStatus(),
                d.getSensitivity(),
                d.getOwner(),
                d.getCreatedAt(),
                d.getUpdatedAt(),
                versions,
                iconUrl,
                d.getReviewedBy(),
                d.getReviewedAt(),
                d.getRejectionReason(),
                d.getPublishedAt(),
                d.getAbrogatedAt()
        );
    }

    private void audit(String action, Long docId, String actor, String details) {
        auditRepo.save(new DocumentAudit(null, docId, action, actor, LocalDateTime.now(), details));
    }

    /* ------------ helpers ------------ */

    private String sanitizeFolderPath(String dbPath) {
        if (dbPath == null || dbPath.isBlank()) return "root";
        String p = dbPath.replaceAll("^/+", "");
        return p.replaceAll("/+", "/");
    }

    private String slug(String s) {
        if (s == null) return "untitled";
        String n = Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}+","");
        n = n.replaceAll("[^a-zA-Z0-9-_ ]","").replace(' ','-');
        n = n.replaceAll("-{2,}", "-");
        return n.isBlank() ? "untitled" : n.toLowerCase();
    }

    private String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) return "file";
        int dot = original.lastIndexOf('.');
        String ext  = (dot < 0) ? "" : original.substring(dot).replaceAll("[^a-zA-Z0-9.]", "").toLowerCase();
        String base = (dot < 0) ? original : original.substring(0, dot);
        String safeBase = slug(base);
        return ext.isEmpty() ? safeBase : safeBase + ext;
    }

    private String uiDocListLink(Long docId, Folder folder) {
        return (folder != null)
                ? "/app/docs?folderId=" + folder.getId() + "&docId=" + docId
                : "/app/docs?root=true&docId=" + docId;
    }

    private void safeNotifyRole(String role, String type, String title, String message, String link, String actor) {
        try {
            notificationService.createForRole(role, type, title, message, link, actor);
        } catch (Exception ignored) { }
    }
}

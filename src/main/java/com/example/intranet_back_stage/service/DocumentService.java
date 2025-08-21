package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.DocumentCreateDto;
import com.example.intranet_back_stage.dto.DocumentDto;
import com.example.intranet_back_stage.dto.VersionDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    // icons
    private final FileIconService fileIconService;

    // notifications
    private final NotificationService notificationService;

    // Notification type keys
    private static final String TYPE_DOC_CREATED    = "DOC_CREATED";
    private static final String TYPE_DOC_NEWVER     = "DOC_NEW_VERSION";

    // Role names that should receive doc updates (adjust if yours differ)
    private static final String ROLE_EMPLOYEE       = "USER";
    private static final String ROLE_DOCS_VIEWER    = "DOCS_VIEWER";

    @Transactional
    public DocumentDto create(DocumentCreateDto meta, MultipartFile file, String username) throws Exception {
        Folder folder = (meta.folderId()!=null)
                ? folderRepo.findById(meta.folderId()).orElseThrow(() -> new IllegalArgumentException("Folder not found"))
                : null;

        Document d = new Document();
        d.setFolder(folder);
        d.setTitle(meta.title());                 // keep extension if you send it from FE
        d.setDocType(meta.docType());
        d.setStatus(Optional.ofNullable(meta.status()).orElse("BROUILLON"));
        d.setSensitivity(Optional.ofNullable(meta.sensitivity()).orElse("INTERNE"));
        d.setOwner(username);
        d.setCreatedAt(LocalDateTime.now());
        d = docRepo.save(d);

        addVersionInternal(d, file, "1.0", username, "Initial upload");
        audit("CREATE", d.getId(), username, "Created with 1.0");

        // 🔔 notify employees that a new document is available
        String title = "Nouveau document: " + d.getTitle();
        String message = "Ajouté" + (folder != null ? " dans " + folder.getPath() : " à la racine") + ".";
        String link = uiDocListLink(d.getId(), folder);
        // fan-out to typical employee roles
        safeNotifyRole(ROLE_EMPLOYEE, TYPE_DOC_CREATED, title, message, link, username);
        safeNotifyRole(ROLE_DOCS_VIEWER, TYPE_DOC_CREATED, title, message, link, username);

        return toDto(d);
    }

    public Page<DocumentDto> getAll(Pageable p) {
        return docRepo.findAll(p).map(this::toDto);
    }

    public Page<DocumentDto> getByFolder(Long folderId, Pageable p, String q) {
        String query = (q == null) ? "" : q.trim();
        Specification<Document> spec = (root, cq, cb) -> {
            var folderExpr = root.get("folder");
            var inFolder = (folderId == null) ? cb.isNull(folderExpr) : cb.equal(folderExpr.get("id"), folderId);
            if (query.isEmpty()) return inFolder;
            var titleLike = cb.like(cb.lower(root.get("title")), "%" + query.toLowerCase() + "%");
            var typeLike  = cb.like(cb.lower(root.get("docType")), "%" + query.toLowerCase() + "%");
            return cb.and(inFolder, cb.or(titleLike, typeLike));
        };
        return docRepo.findAll(spec, p).map(this::toDto);
    }

    public DocumentDto getById(Long id) {
        Document d = docRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + id));
        return toDto(d);
    }

    @Transactional
    public VersionDto addVersion(Long docId, MultipartFile file, String versionNo, String username, String comment) throws Exception {
        Document d = docRepo.findById(docId).orElseThrow();
        VersionDto v = addVersionInternal(d, file, versionNo, username, comment);
        audit("UPDATE", d.getId(), username, "New version " + versionNo);

        // 🔔 notify employees about the new version
        String nTitle = "Nouvelle version: " + d.getTitle() + " (v" + versionNo + ")";
        String nMsg   = "Ajoutée par " + username + ".";
        String link   = uiDocListLink(d.getId(), d.getFolder());
        safeNotifyRole(ROLE_EMPLOYEE, TYPE_DOC_NEWVER, nTitle, nMsg, link, username);
        safeNotifyRole(ROLE_DOCS_VIEWER, TYPE_DOC_NEWVER, nTitle, nMsg, link, username);

        return v;
    }

    private VersionDto addVersionInternal(Document d, MultipartFile file, String versionNo, String username, String comment) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("File required");

        String folderPath = (d.getFolder() == null)
                ? "root"
                : sanitizeFolderPath(d.getFolder().getPath());
        String titleSlug = slug(d.getTitle());
        String safeOriginal = sanitizeFilename(file.getOriginalFilename());

        String objectKey = String.format("docs/%s/%s/v%s/%s",
                folderPath, titleSlug, versionNo, safeOriginal);

        String key = storage.put(
                file.getInputStream(),
                file.getSize(),
                file.getContentType(),
                objectKey
        );

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

    public Page<DocumentDto> search(String q, Pageable p) {
        return docRepo.findByTitleContainingIgnoreCaseOrDocTypeContainingIgnoreCase(q, q, p)
                .map(this::toDto);
    }

    private DocumentDto toDto(Document d) {
        List<VersionDto> versions = verRepo.findByDocumentIdOrderByCreatedAtDesc(d.getId()).stream()
                .map(v -> new VersionDto(v.getId(), v.getVersionNo(), v.getFilename(), v.getSize(), v.getCreatedBy(), v.getCreatedAt()))
                .toList();

        String latestFilename = versions.isEmpty() ? null : versions.get(0).filename();
        String iconUrl = fileIconService.iconFor(d.getDocType(), latestFilename);

        return new DocumentDto(
                d.getId(), d.getTitle(), d.getDocType(), d.getStatus(), d.getSensitivity(),
                d.getOwner(), d.getCreatedAt(), d.getUpdatedAt(), versions, iconUrl
        );
    }

    private void audit(String action, Long docId, String actor, String details) {
        auditRepo.save(new DocumentAudit(null, docId, action, actor, LocalDateTime.now(), details));
    }

    @Transactional
    public void delete(Long docId, String username) {
        Document d = docRepo.findById(docId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + docId));

        List<DocumentVersion> versions = verRepo.findByDocumentIdOrderByCreatedAtDesc(docId);

        for (DocumentVersion v : versions) {
            try {
                if (v.getStorageKey() != null) storage.delete(v.getStorageKey());
            } catch (Exception ex) {
                System.err.println("Failed to delete blob: " + v.getStorageKey() + " -> " + ex.getMessage());
            }
        }

        verRepo.deleteAll(versions);
        docRepo.delete(d);
        audit("DELETE", docId, username, "Document and " + versions.size() + " version(s) removed");

        // (Optional) you can notify HR/Admin about deletions if needed.
        // notificationService.createForHrAndAdmin(...);
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
        n = n.replaceAll("[^a-zA-Z0-9-_ ]","").replace(' ','-'); // ✅ fix here
        n = n.replaceAll("-{2,}", "-");
        return n.isBlank() ? "untitled" : n.toLowerCase();
    }

    private String sanitizeFilename(String original) {
        if (original == null || original.isBlank()) return "file";
        String ext = getExtension(original);
        String base = original.substring(0, original.length() - ext.length());
        String safeBase = slug(base);
        return ext.isEmpty() ? safeBase : safeBase + ext.toLowerCase();
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) return "";
        return filename.substring(dot).replaceAll("[^a-zA-Z0-9.]", "").toLowerCase();
    }

    private String uiDocListLink(Long docId, Folder folder) {
        // Adjust to your Angular routes.
        // Example: a list page that can highlight a doc by query param:
        //   /app/docs?docId=123 or /app/docs?folderId=456&docId=123
        if (folder != null) {
            return "/app/docs?folderId=" + folder.getId() + "&docId=" + docId;
        }
        return "/app/docs?root=true&docId=" + docId;
    }

    private void safeNotifyRole(String role, String type, String title, String message, String link, String actor) {
        try {
            notificationService.createForRole(role, type, title, message, link, actor);
        } catch (Exception ignored) {
            // swallow to avoid breaking document flows if role doesn't exist in some envs
        }
    }
}

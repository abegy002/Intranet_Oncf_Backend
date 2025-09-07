package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.AttestationRequestDTO;
import com.example.intranet_back_stage.dto.AttestationRequestResponse;
import com.example.intranet_back_stage.model.AttestationRequest;
import com.example.intranet_back_stage.model.User;
import com.example.intranet_back_stage.repository.AttestationRequestRepository;
import com.example.intranet_back_stage.repository.UserRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.time.Duration;          // ⬅️ ajouté
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AttestationService {

    private final AttestationRequestRepository requestRepo;
    private final UserRepository userRepo;

    private final NotificationService notificationService;

    // 🗄️ Storage générique (MinIO / filesystem selon config)
    private final StorageService storage;

    private static final String TYPE_ATTESTATION_SUBMITTED   = "ATTESTATION_SUBMITTED";
    private static final String TYPE_ATTESTATION_IN_PROGRESS = "ATTESTATION_IN_PROGRESS";
    private static final String TYPE_ATTESTATION_SENT        = "ATTESTATION_SENT";
    private static final String TYPE_ATTESTATION_REJECTED    = "ATTESTATION_REJECTED";

    /* =========================== Commands =========================== */

    public AttestationRequest submit(AttestationRequestDTO dto) {
        User employee = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        AttestationRequest req = new AttestationRequest();
        req.setEmployee(employee);
        req.setAttestationType(dto.getAttestationType());
        req.setStatus(AttestationRequest.AttestationStatus.EN_ATTENTE);
        req.setCreatedAt(LocalDateTime.now());

        req = requestRepo.save(req);

        // 🔔 Managers
        String hrAdminTitle = "Nouvelle demande d’attestation";
        String hrAdminMsg = String.format(
                "%s %s a soumis une demande d’attestation (%s).",
                employee.getFirstname(), employee.getLastname(), dto.getAttestationType().name()
        );
        notificationService.createForHrAndAdmin(
                TYPE_ATTESTATION_SUBMITTED,
                hrAdminTitle,
                hrAdminMsg,
                uiRequestDetailUrlForManagers(req.getId()),
                employee.getUsername()
        );

        // 🔔 Accusé réception employé
        notificationService.createForUser(
                employee.getId(),
                employee.getUsername(),
                TYPE_ATTESTATION_SUBMITTED,
                "Votre demande d’attestation a été soumise",
                "Nous vous informerons dès qu’elle sera prise en charge.",
                employee.getUsername()
        );

        return req;
    }

    public AttestationRequest process(Long id, String processor) {
        AttestationRequest req = findByIdOrThrow(id);
        if (req.getStatus() == AttestationRequest.AttestationStatus.REJETE) {
            throw new IllegalStateException("Cette demande a été rejetée et ne peut plus être modifiée.");
        }
        req.setStatus(AttestationRequest.AttestationStatus.EN_COURS);
        req.setProcessedBy(processor);
        req.setProcessedAt(LocalDateTime.now());
        req = requestRepo.save(req);

        // 🔔 Notif employé
        User employee = req.getEmployee();
        notificationService.createForUser(
                employee.getId(),
                employee.getUsername(),
                TYPE_ATTESTATION_IN_PROGRESS,
                "Votre demande d’attestation est en cours de traitement",
                "Un RH/Administrateur a pris en charge votre demande.",
                processor
        );
        return req;
    }

    /** RH charge le PDF signé -> ENVOYE (stocké dans MinIO via StorageService) */
    public AttestationRequest sendWithSignedPdf(Long id, MultipartFile file) {
        AttestationRequest req = findByIdOrThrow(id);

        if (req.getStatus() == AttestationRequest.AttestationStatus.REJETE) {
            throw new IllegalStateException("Cette demande a été rejetée et ne peut plus être modifiée.");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier signé est requis.");
        }
        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            throw new IllegalArgumentException("Le fichier doit être un PDF.");
        }

        try {
            // Nom « propre »
            String safeName = sanitizePdfFilename(file.getOriginalFilename());

            // Chemin/clé stable dans MinIO
            User employee = req.getEmployee();
            String who = (employee.getEmployeeCode() != null && !employee.getEmployeeCode().isBlank())
                    ? employee.getEmployeeCode()
                    : String.valueOf(employee.getId());

            LocalDate now = LocalDate.now();
            String objectKey = String.format(
                    "attestations/signed/%s/%04d/%02d/attestation_%d_%d.pdf",
                    who, now.getYear(), now.getMonthValue(), req.getId(), System.currentTimeMillis()
            );

            // Upload dans MinIO (ou autre impl StorageService)
            String storageKey = storage.put(
                    file.getInputStream(),
                    file.getSize(),
                    "application/pdf",
                    objectKey
            );

            // Persistance des métadonnées
            req.setSignedDocumentPath(storageKey);   // ⬅️ clé MinIO
            req.setSignedDocumentFilename(safeName); // ⬅️ nom d’affichage
            req.setStatus(AttestationRequest.AttestationStatus.ENVOYE);
            req.setSentAt(LocalDateTime.now());

            req = requestRepo.save(req);

            // 🔔 Notif employé
            String title = "Votre attestation est prête";
            String msg = "Votre attestation signée a été envoyée. Cliquez pour la télécharger.";
            notificationService.createForUser(
                    employee.getId(),
                    employee.getUsername(),
                    TYPE_ATTESTATION_SENT,
                    title,
                    msg,
                    req.getProcessedBy() != null ? req.getProcessedBy() : "system"
            );

            return req;

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du fichier signé : " + e.getMessage(), e);
        }
    }

    public AttestationRequest reject(Long id) {
        AttestationRequest req = findByIdOrThrow(id);
        req.setStatus(AttestationRequest.AttestationStatus.REJETE);
        req = requestRepo.save(req);

        // 🔔 Notif employé
        User employee = req.getEmployee();
        notificationService.createForUser(
                employee.getId(),
                employee.getUsername(),
                TYPE_ATTESTATION_REJECTED,
                "Votre demande d’attestation a été rejetée",
                "Veuillez contacter le service RH pour plus d’informations.",
                req.getProcessedBy() != null ? req.getProcessedBy() : "system"
        );
        return req;
    }

    /* =========================== Queries =========================== */

    public List<AttestationRequestResponse> getAllRequests() {
        return requestRepo.findAll().stream()
                .map(r -> new AttestationRequestResponse(
                        r.getId(),
                        r.getAttestationType().name(),
                        r.getStatus().name(),
                        r.getEmployee().getFirstname(),
                        r.getEmployee().getLastname()
                ))
                .toList();
    }

    public List<AttestationRequest> getByEmployee(Long userId) {
        return requestRepo.findByEmployeeId(userId);
    }

    public AttestationRequest findByIdOrThrow(Long id) {
        return requestRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
    }

    /* =========================== PDF Generation =========================== */

    public byte[] generatePdfAndReturnBytes(Long requestId) {
        AttestationRequest request = findByIdOrThrow(requestId);
        if (request.getStatus() == AttestationRequest.AttestationStatus.REJETE) {
            throw new IllegalStateException("Cette demande a été rejetée et ne peut plus être modifiée.");
        }

        User user = request.getEmployee();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            try {
                ClassPathResource logoRes = new ClassPathResource("static/logo/Logo-oncf.png");
                byte[] logoBytes = logoRes.getInputStream().readAllBytes();
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(100, 100);
                logo.setAlignment(Image.ALIGN_CENTER);
                document.add(logo);
            } catch (Exception ignored) { }

            Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font normal    = new Font(Font.HELVETICA, 12);
            Font bold      = new Font(Font.HELVETICA, 12, Font.BOLD);

            String titleText = request.getAttestationType() == AttestationRequest.AttestationType.SALAIRE
                    ? "ATTESTATION DE SALAIRE"
                    : "ATTESTATION DE TRAVAIL";

            Paragraph title = new Paragraph(titleText, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(20);
            title.setSpacingAfter(30);
            document.add(title);

            Paragraph body = new Paragraph();
            body.setAlignment(Element.ALIGN_JUSTIFIED);
            body.setSpacingAfter(30);

            if (request.getAttestationType() == AttestationRequest.AttestationType.SALAIRE) {
                BigDecimal salaireBrut = user.getSalaire() != null ? user.getSalaire() : BigDecimal.ZERO;
                BigDecimal retenue     = salaireBrut.multiply(new BigDecimal("0.15"));
                BigDecimal net         = salaireBrut.subtract(retenue);

                body.add(new Chunk("Nous soussignés, ", normal));
                body.add(new Chunk("M. Karim EL MANSOURI, ", bold));
                body.add(new Chunk("Responsable des Ressources Humaines de l’", normal));
                body.add(new Chunk("Office National des Chemins de Fer (ONCF)", bold));
                body.add(new Chunk(", attestons que ", normal));
                body.add(new Chunk(("Mr./Mme. " + user.getFirstname() + " " + user.getLastname()).toUpperCase(), bold));
                body.add(new Chunk(" est employé(e) en qualité de ", normal));
                body.add(new Chunk(user.getJob().getTitle(), bold));
                body.add(new Chunk(".\n\nSalaire brut : ", normal));
                body.add(new Chunk(formatMoney(salaireBrut), bold));
                body.add(new Chunk(", Retenues : ", normal));
                body.add(new Chunk(formatMoney(retenue), bold));
                body.add(new Chunk(", Salaire net : ", normal));
                body.add(new Chunk(formatMoney(net), bold));
            } else {
                body.add(new Chunk("Nous soussignés, ", normal));
                body.add(new Chunk("M. Karim EL MANSOURI, ", bold));
                body.add(new Chunk("Responsable des Ressources Humaines de l’", normal));
                body.add(new Chunk("Office National des Chemins de Fer (ONCF)", bold));
                body.add(new Chunk(", attestons que ", normal));
                body.add(new Chunk(("Mr./Mme. " + user.getFirstname() + " " + user.getLastname()).toUpperCase(), bold));
                body.add(new Chunk(" est employé(e) en qualité de ", normal));
                body.add(new Chunk(user.getJob().getTitle(), bold));
                body.add(new Chunk(" depuis le ", normal));
                body.add(new Chunk(
                        request.getCreatedAt().toLocalDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)),
                        bold
                ));
                body.add(new Chunk(". ", normal));
                body.add(new Chunk("Cette attestation est délivrée à la demande de l’intéressé(e).", normal));
            }

            document.add(body);

            Paragraph datePlace = new Paragraph(
                    "Fait à Rabat, le " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH)),
                    normal
            );
            datePlace.setAlignment(Element.ALIGN_RIGHT);
            document.add(datePlace);

            Paragraph sign = new Paragraph("\nResponsable des Ressources Humaines\n\n", bold);
            sign.setAlignment(Element.ALIGN_RIGHT);
            sign.setSpacingBefore(30);
            document.add(sign);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF : " + e.getMessage(), e);
        }
    }

    /* =========================== File access =========================== */

    /**
     * Retourne une Resource basée sur une **URL présignée** MinIO (TTL 10 min).
     * Idéale si votre contrôleur fait un simple `return ResponseEntity.ok(resource)` ou une redirection.
     */
    public Resource loadSignedPdfAsResource(Long id) {
        AttestationRequest req = findByIdOrThrow(id);
        if (req.getSignedDocumentPath() == null) {
            throw new IllegalStateException("Aucun fichier signé n'est associé à cette demande.");
        }
        try {
            URL url = storage.getSignedUrl(req.getSignedDocumentPath(), Duration.ofMinutes(10));
            return new UrlResource(url);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la création de l'URL signée : " + e.getMessage(), e);
        }
    }

    /* =========================== Helpers =========================== */

    private String formatMoney(BigDecimal amount) {
        return String.format(Locale.FRANCE, "%,.2f MAD", amount);
    }

    @SuppressWarnings("unused")
    private void saveUnsignedForTrace(Long requestId, byte[] bytes) {
        try {
            Path unsignedDir = Paths.get("src/main/resources/static/attestations/unsigned").toAbsolutePath();
            Files.createDirectories(unsignedDir);
            String fileName = "attestation_" + requestId + "_unsigned.pdf";
            Path file = unsignedDir.resolve(fileName);
            try (FileOutputStream fos = new FileOutputStream(file.toFile())) {
                fos.write(bytes);
            }
        } catch (Exception ignored) { }
    }

    /* =========================== URL builders =========================== */

    private String uiRequestDetailUrlForManagers(Long reqId) {
        return "/app/attestations/requests/" + reqId;
    }

    private String uiRequestDetailUrlForEmployee(Long reqId) {
        return "/app/attestations/mine/" + reqId;
    }

    private String uiMyRequestsUrl() {
        return "/app/attestations/mine";
    }

    private String apiSignedDownloadUrl(Long reqId) {
        return "/attestations/signed/" + reqId;
    }

    /* =========================== Extras =========================== */

    /** (Optionnel) URL présignée simple si vous voulez renvoyer une String depuis un endpoint. */
    public String signedUrl(Long requestId, Duration ttl) {
        AttestationRequest req = findByIdOrThrow(requestId);
        if (req.getSignedDocumentPath() == null || req.getSignedDocumentPath().isBlank()) {
            throw new IllegalStateException("Aucun PDF signé associé.");
        }
        try {
            return storage.getSignedUrl(req.getSignedDocumentPath(), ttl != null ? ttl : Duration.ofMinutes(10)).toExternalForm();
        } catch (Exception e) {
            throw new RuntimeException("Erreur génération URL signée : " + e.getMessage(), e);
        }
    }

    /** Nettoyage minimal pour nom PDF. */
    private String sanitizePdfFilename(String original) {
        String fallback = "attestation.pdf";
        if (original == null || original.isBlank()) return fallback;
        String name = original.replace("\\", "/");
        int slash = name.lastIndexOf('/');
        if (slash >= 0) name = name.substring(slash + 1);
        if (!name.toLowerCase().endsWith(".pdf")) name = name + ".pdf";
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        return name.isBlank() ? fallback : name;
    }
}

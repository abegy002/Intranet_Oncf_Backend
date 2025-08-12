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
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
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

    /* =========================== Commands =========================== */

    public AttestationRequest submit(AttestationRequestDTO dto) {
        User employee = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        AttestationRequest req = new AttestationRequest();
        req.setEmployee(employee);
        req.setAttestationType(dto.getAttestationType());
        req.setStatus(AttestationRequest.AttestationStatus.EN_ATTENTE);
        req.setCreatedAt(LocalDateTime.now());

        return requestRepo.save(req);
    }

    public AttestationRequest process(Long id, String processor) {
        AttestationRequest req = requestRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        if (req.getStatus() == AttestationRequest.AttestationStatus.REJETE) {
            throw new IllegalStateException("Cette demande a été rejetée et ne peut plus être modifiée.");
        }

        req.setStatus(AttestationRequest.AttestationStatus.EN_COURS);
        req.setProcessedBy(processor);
        req.setProcessedAt(LocalDateTime.now());
        return requestRepo.save(req);
    }

    /** New logic: HR uploads the scanned & signed PDF on send */
    public AttestationRequest sendWithSignedPdf(Long id, MultipartFile file) {
        AttestationRequest req = requestRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

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
            // Save inside project (dev-friendly): src/main/resources/static/attestations/signed
            Path signedDir = Paths.get("src/main/resources/static/attestations/signed").toAbsolutePath();
            Files.createDirectories(signedDir);

            String filename = "attestation_" + id + "_signed_" + System.currentTimeMillis() + ".pdf";
            Path target = signedDir.resolve(filename);

            Files.copy(file.getInputStream(), target);

            req.setSignedDocumentPath(target.toString());
            req.setSignedDocumentFilename(filename);

            // Set status to ENVOYE once signed file uploaded
            req.setStatus(AttestationRequest.AttestationStatus.ENVOYE);
            req.setSentAt(LocalDateTime.now());

            return requestRepo.save(req);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du fichier signé : " + e.getMessage(), e);
        }
    }

    public AttestationRequest reject(Long id) {
        AttestationRequest req = requestRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        req.setStatus(AttestationRequest.AttestationStatus.REJETE);
        return requestRepo.save(req);
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

    /* =========================== PDF Generation =========================== */

    /**
     * Generates an UNSIGNED PDF and returns bytes.
     * Does NOT touch DB state. (HR will sign manually offline.)
     */
    public byte[] generatePdfAndReturnBytes(Long requestId) {
        AttestationRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() == AttestationRequest.AttestationStatus.REJETE) {
            throw new IllegalStateException("Cette demande a été rejetée et ne peut plus être modifiée.");
        }

        User user = request.getEmployee();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            // --- Logo from classpath: place your logo in src/main/resources/static/logo/Logo-oncf.png ---
            try {
                ClassPathResource logoRes = new ClassPathResource("static/logo/Logo-oncf.png");
                byte[] logoBytes = logoRes.getInputStream().readAllBytes();
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(100, 100);
                logo.setAlignment(Image.ALIGN_CENTER);
                document.add(logo);
            } catch (Exception logoEx) {
                // If logo missing, continue without failing the whole PDF
            }

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

            // (Intentionally no signature image; HR will sign manually after printing)
            Paragraph sign = new Paragraph("\nResponsable des Ressources Humaines\n\n", bold);
            sign.setAlignment(Element.ALIGN_RIGHT);
            sign.setSpacingBefore(30);
            document.add(sign);

            document.close();

            // Optionally, you could also keep a local unsigned copy if you want:
            // saveUnsignedForTrace(request.getId(), baos.toByteArray());

            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF : " + e.getMessage(), e);
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
        } catch (Exception ignored) {
        }
    }

    public Resource loadSignedPdfAsResource(Long id) {
        AttestationRequest req = requestRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));

        if (req.getSignedDocumentPath() == null) {
            throw new IllegalStateException("Aucun fichier signé n'est associé à cette demande.");
        }

        try {
            Path path = Paths.get(req.getSignedDocumentPath()).toAbsolutePath().normalize();
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new IllegalStateException("Fichier signé introuvable ou illisible.");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Chemin de fichier invalide : " + e.getMessage(), e);
        }
    }
}

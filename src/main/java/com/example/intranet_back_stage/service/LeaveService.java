package com.example.intranet_back_stage.service;

import com.example.intranet_back_stage.dto.LeaveRequestCreateDTO;
import com.example.intranet_back_stage.dto.LeaveRequestRHCreateDTO;
import com.example.intranet_back_stage.dto.LeaveRequestResponse;
import com.example.intranet_back_stage.model.LeaveBalance;
import com.example.intranet_back_stage.model.LeaveRequest;
import com.example.intranet_back_stage.model.User;
import com.example.intranet_back_stage.repository.LeaveBalanceRepository;
import com.example.intranet_back_stage.repository.LeaveRequestRepository;
import com.example.intranet_back_stage.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private final LeaveRequestRepository leaveRepo;
    private final LeaveBalanceRepository balanceRepo;
    private final UserRepository userRepo;

    // ⬇️ Notifications
    private final NotificationService notificationService;

    private static final BigDecimal DEFAULT_ANNUAL_ENTITLEMENT = new BigDecimal("22.00");
    private static final int MIN_ADVANCE_DAYS_EMPLOYEE = 7; // 1 semaine

    // Notification types (free-form keys)
    private static final String TYPE_LEAVE_SUBMITTED = "LEAVE_SUBMITTED";
    private static final String TYPE_LEAVE_ACK = "LEAVE_ACK";
    private static final String TYPE_LEAVE_AUTO_APPROVED = "LEAVE_AUTO_APPROVED";
    private static final String TYPE_LEAVE_APPROVED = "LEAVE_APPROVED";
    private static final String TYPE_LEAVE_REJECTED = "LEAVE_REJECTED";
    private static final String TYPE_LEAVE_CANCELLED = "LEAVE_CANCELLED";

    /* ================= Helpers ================= */

    private boolean isPlanifiable(LeaveRequest.LeaveType t) {
        return switch (t) {
            case ANNUEL, EXCEPTIONNEL -> true;
            default -> false;
        };
    }

    private boolean isNonPlanifiable(LeaveRequest.LeaveType t) {
        return switch (t) {
            case MALADIE, MATERNITE, PATERNITE, ACCIDENT, HOSPITALISATION -> true;
            default -> false;
        };
    }

    /** Jours calendaires simples (votre logique existante). */
    private BigDecimal workingDays(LocalDate start, LocalDate end) {
        long days = (end.toEpochDay() - start.toEpochDay()) + 1;
        return BigDecimal.valueOf(days);
    }

    private LeaveBalance getOrInitBalance(User u, int year, BigDecimal defaultEntitled) {
        return balanceRepo.findByEmployeeIdAndYear(u.getId(), year)
                .orElseGet(() -> balanceRepo.save(new LeaveBalance(
                        null, u, year, defaultEntitled, BigDecimal.ZERO
                )));
    }

    private LeaveRequestResponse toResponse(LeaveRequest r) {
        return new LeaveRequestResponse(
                r.getId(),
                r.getType() != null ? r.getType().name() : null,
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getStartDate(),
                r.getEndDate(),
                r.getReason(),
                r.getJustificatifFilename(),
                r.getCreatedAt(),
                r.getDecidedAt(),
                r.getDecidedBy(),
                r.getEmployee() != null
                        ? (r.getEmployee().getFirstname() + " " + r.getEmployee().getLastname())
                        : null
        );
    }

    /* ================= Employé : crée une demande ================= */

    public LeaveRequest createByEmployee(LeaveRequestCreateDTO dto) {
        User emp = userRepo.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable"));

        // Blocage maternité/paternité
        if (dto.getType() == LeaveRequest.LeaveType.MATERNITE || dto.getType() == LeaveRequest.LeaveType.PATERNITE) {
            throw new IllegalStateException("Les congés maternité et paternité doivent être saisis par le service RH.");
        }

        if (!isPlanifiable(dto.getType())) {
            throw new IllegalStateException("Ce type de congé doit être saisi par le RH.");
        }

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("Dates invalides (fin avant début).");
        }

        // Règle : minimum 7 jours avant
        LocalDate minStartDate = LocalDate.now().plusDays(MIN_ADVANCE_DAYS_EMPLOYEE);
        if (dto.getStartDate().isBefore(minStartDate)) {
            throw new IllegalArgumentException(
                    "La date de début doit être au moins " + MIN_ADVANCE_DAYS_EMPLOYEE +
                            " jours après aujourd'hui (" + minStartDate + ")."
            );
        }

        // Contrôle du solde pour congés annuels
        if (dto.getType() == LeaveRequest.LeaveType.ANNUEL) {
            BigDecimal days = workingDays(dto.getStartDate(), dto.getEndDate());

            LeaveBalance bal = getOrInitBalance(emp, dto.getStartDate().getYear(), DEFAULT_ANNUAL_ENTITLEMENT);
            BigDecimal entitled  = bal.getAnnualEntitled() == null ? DEFAULT_ANNUAL_ENTITLEMENT : bal.getAnnualEntitled();
            BigDecimal used      = bal.getAnnualUsed() == null ? BigDecimal.ZERO : bal.getAnnualUsed();
            BigDecimal remaining = entitled.subtract(used);

            if (days.compareTo(remaining) > 0) {
                throw new IllegalStateException(
                        "Votre solde annuel est insuffisant : il reste " +
                                remaining.stripTrailingZeros().toPlainString() + " jour(s) sur " +
                                entitled.stripTrailingZeros().toPlainString() + " pour l’année " +
                                dto.getStartDate().getYear() + "."
                );
            }
        }

        LeaveRequest r = new LeaveRequest();
        r.setEmployee(emp);
        r.setType(dto.getType());
        r.setStartDate(dto.getStartDate());
        r.setEndDate(dto.getEndDate());
        r.setReason(dto.getReason());
        r.setStatus(LeaveRequest.LeaveStatus.EN_ATTENTE);
        r.setCreatedAt(LocalDateTime.now());
        r = leaveRepo.save(r);

        // 🔔 Notify HR + ADMIN
        String period = formatPeriod(r.getStartDate(), r.getEndDate());
        String titleManagers = "Nouvelle demande de congé (" + r.getType().name() + ")";
        String msgManagers = emp.getFirstname() + " " + emp.getLastname() + " a soumis une demande " + period + ".";
        notificationService.createForHrAndAdmin(
                TYPE_LEAVE_SUBMITTED,
                titleManagers,
                msgManagers,
                uiLeaveDetailUrlForManagers(r.getId()),
                emp.getUsername()
        );

        // 🔔 Ack employee
        notificationService.createForUser(
                emp.getId(), emp.getUsername(),
                TYPE_LEAVE_ACK,
                "Votre demande de congé a été soumise",
                "Période " + period + ". Nous vous informerons dès qu’elle sera traitée.",
                emp.getUsername()
        );

        return r;
    }

    /* =========== RH : enregistre un non planifiable =========== */

    /**
     * Les types MALADIE, MATERNITE, PATERNITE, ACCIDENT, HOSPITALISATION
     * sont automatiquement APPROUVÉS s’il y a un justificatif PDF fourni; sinon EN_ATTENTE.
     */
    public LeaveRequest createByRh(LeaveRequestRHCreateDTO dto, MultipartFile justificatif) {
        User employee = userRepo.findByEmployeeCode(dto.getEmployeeCode())
                .orElseThrow(() -> new IllegalArgumentException("Employé introuvable pour le code: " + dto.getEmployeeCode()));

        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new IllegalArgumentException("Dates invalides (fin avant début).");
        }

        LeaveRequest lr = new LeaveRequest();
        lr.setEmployee(employee);
        lr.setType(dto.getType());
        lr.setStartDate(dto.getStartDate());
        lr.setEndDate(dto.getEndDate());
        lr.setReason(dto.getReason());
        lr.setCreatedAt(LocalDateTime.now());
        lr.setStatus(LeaveRequest.LeaveStatus.EN_ATTENTE); // défaut

        // Sauvegarde justificatif si fourni
        boolean hasValidJustif = false;
        if (justificatif != null && !justificatif.isEmpty()) {
            if (!"application/pdf".equalsIgnoreCase(justificatif.getContentType())) {
                throw new IllegalArgumentException("Le justificatif doit être un PDF.");
            }
            try {
                Path dir = Paths.get("src/main/resources/static/leaves/justifs").toAbsolutePath();
                Files.createDirectories(dir);
                String filename = "justif_" + employee.getId() + "_" + System.currentTimeMillis() + ".pdf";
                Path target = dir.resolve(filename);
                Files.copy(justificatif.getInputStream(), target);
                lr.setJustificatifPath(target.toString());
                lr.setJustificatifFilename(filename);
                hasValidJustif = true;
            } catch (Exception e) {
                throw new RuntimeException("Erreur lors de l'enregistrement du justificatif: " + e.getMessage(), e);
            }
        }

        // Auto-approbation si non planifiable + justificatif fourni
        if (isNonPlanifiable(dto.getType()) && hasValidJustif) {
            lr.setStatus(LeaveRequest.LeaveStatus.APPROUVE);
            lr.setDecidedAt(LocalDateTime.now());
            lr.setDecidedBy("RH"); // adaptez avec l’utilisateur connecté
        }

        lr = leaveRepo.save(lr);

        // 🔔 Notify employee about the RH entry
        String period = formatPeriod(lr.getStartDate(), lr.getEndDate());
        if (lr.getStatus() == LeaveRequest.LeaveStatus.APPROUVE) {
            notificationService.createForUser(
                    employee.getId(), employee.getUsername(),
                    TYPE_LEAVE_AUTO_APPROVED,
                    "Votre congé a été approuvé",
                    "Type " + lr.getType().name() + ", période " + period + ".",
                    "RH"
            );
        } else {
            notificationService.createForUser(
                    employee.getId(), employee.getUsername(),
                    TYPE_LEAVE_SUBMITTED,
                    "Votre demande a été enregistrée par le RH",
                    "Type " + lr.getType().name() + ", période " + period + ". En attente de décision.",
                    "RH"
            );
        }

        return lr;
    }

    /* =========== RH : approuve / rejette une demande planifiable =========== */

    public LeaveRequest approve(Long id, String rhUsername) {
        LeaveRequest r = leaveRepo.findById(id).orElseThrow();
        if (r.getStatus() != LeaveRequest.LeaveStatus.EN_ATTENTE) {
            throw new IllegalStateException("Demande non éligible à l’approbation.");
        }

        // Plafond strict pour congés annuels
        if (r.getType() == LeaveRequest.LeaveType.ANNUEL) {
            BigDecimal days = workingDays(r.getStartDate(), r.getEndDate());

            LeaveBalance bal = getOrInitBalance(r.getEmployee(), r.getStartDate().getYear(), DEFAULT_ANNUAL_ENTITLEMENT);
            BigDecimal entitled  = bal.getAnnualEntitled() == null ? DEFAULT_ANNUAL_ENTITLEMENT : bal.getAnnualEntitled();
            BigDecimal used      = bal.getAnnualUsed() == null ? BigDecimal.ZERO : bal.getAnnualUsed();
            BigDecimal remaining = entitled.subtract(used);

            if (days.compareTo(remaining) > 0) {
                throw new IllegalStateException(
                        "Solde insuffisant : il reste " +
                                remaining.stripTrailingZeros().toPlainString() + " jour(s) sur " +
                                entitled.stripTrailingZeros().toPlainString() + " pour l’année " +
                                r.getStartDate().getYear() + "."
                );
            }

            r.setApprovedDays(days);
            bal.setAnnualUsed(used.add(days));
            balanceRepo.save(bal);
        }

        r.setStatus(LeaveRequest.LeaveStatus.APPROUVE);
        r.setDecidedAt(LocalDateTime.now());
        r.setDecidedBy(rhUsername);
        r = leaveRepo.save(r);

        // 🔔 Notify employee
        notificationService.createForUser(
                r.getEmployee().getId(), r.getEmployee().getUsername(),
                TYPE_LEAVE_APPROVED,
                "Votre demande de congé a été approuvée",
                "Période " + formatPeriod(r.getStartDate(), r.getEndDate()) + ".",
                rhUsername
        );

        return r;
    }

    public LeaveRequest reject(Long id, String rhUsername, String reason) {
        LeaveRequest r = leaveRepo.findById(id).orElseThrow();
        if (r.getStatus() != LeaveRequest.LeaveStatus.EN_ATTENTE) {
            throw new IllegalStateException("Demande non éligible au rejet.");
        }
        r.setStatus(LeaveRequest.LeaveStatus.REJETE);
        r.setDecidedAt(LocalDateTime.now());
        r.setDecidedBy(rhUsername);
        if (reason != null && !reason.isBlank()) {
            r.setReason((r.getReason() == null ? "" : r.getReason() + " | ") + "RH: " + reason);
        }
        r = leaveRepo.save(r);

        // 🔔 Notify employee
        notificationService.createForUser(
                r.getEmployee().getId(), r.getEmployee().getUsername(),
                TYPE_LEAVE_REJECTED,
                "Votre demande de congé a été rejetée",
                "Cliquez pour voir le détail (motif éventuel).",
                rhUsername
        );

        return r;
    }

    public LeaveRequest cancel(Long id, Long employeeId) {
        LeaveRequest r = leaveRepo.findById(id).orElseThrow();
        if (!r.getEmployee().getId().equals(employeeId)) {
            throw new IllegalStateException("Non autorisé.");
        }
        if (r.getStatus() != LeaveRequest.LeaveStatus.EN_ATTENTE) {
            throw new IllegalStateException("Seules les demandes en attente peuvent être annulées.");
        }
        r.setStatus(LeaveRequest.LeaveStatus.ANNULE);
        r = leaveRepo.save(r);

        // 🔔 Notify HR + ADMIN about the cancel
        User emp = r.getEmployee();
        notificationService.createForHrAndAdmin(
                TYPE_LEAVE_CANCELLED,
                "Demande de congé annulée",
                emp.getFirstname() + " " + emp.getLastname() + " a annulé sa demande " +
                        formatPeriod(r.getStartDate(), r.getEndDate()) + ".",
                uiLeaveDetailUrlForManagers(r.getId()),
                emp.getUsername()
        );

        return r;
    }

    /* ================= Queries ================= */

    public List<LeaveRequestResponse> getAllLeaves() {
        return leaveRepo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<LeaveRequest> getMine(Long userId) {
        return leaveRepo.findByEmployeeIdOrderByCreatedAtDesc(userId);
    }

    public List<LeaveRequest> getAllPending() {
        return leaveRepo.findAllByStatus(LeaveRequest.LeaveStatus.EN_ATTENTE, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public LeaveBalance getBalance(Long userId, int year) {
        User u = userRepo.getReferenceById(userId);
        return getOrInitBalance(u, year, DEFAULT_ANNUAL_ENTITLEMENT);
    }

    /* ================= Formatting & URL helpers ================= */

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String formatPeriod(LocalDate start, LocalDate end) {
        return "du " + DF.format(start) + " au " + DF.format(end);
    }

    // Managers/HR view of a request
    private String uiLeaveDetailUrlForManagers(Long id) {
        return "/app/leaves/requests/" + id;
    }

    // Employee view of their request
    private String uiLeaveDetailUrlForEmployee(Long id) {
        return "/app/leaves/mine/" + id;
    }
}

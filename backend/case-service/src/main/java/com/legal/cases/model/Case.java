package com.legal.cases.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cases", indexes = {
        @Index(name = "idx_case_number",     columnList = "case_number"),
        @Index(name = "idx_case_status",     columnList = "status"),
        @Index(name = "idx_case_owner",      columnList = "owner_id"),
        @Index(name = "idx_case_supervisor", columnList = "supervisor_id"),
        @Index(name = "idx_case_deleted",    columnList = "deleted_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Case {

    @Id
    @Column(name = "case_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identificadores ──────────────────────────────────────────────────────

    @Column(name = "case_number", nullable = false, unique = true, length = 50)
    private String caseNumber;              // Interno: CASE-2024-00123

    @Column(name = "expedient_number", length = 100)
    private String expedientNumber;         // BR#8 — Número de expediente OBLIGATORIO

    // ── Información general ──────────────────────────────────────────────────

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CaseStatus status = CaseStatus.CREATED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CasePriority priority = CasePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false, length = 30)
    private CaseType caseType;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "tags", length = 500)
    private String tags;                    // JSON string o CSV

    // ── Asignación ───────────────────────────────────────────────────────────

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;                   // Dueño / responsable principal

    @Column(name = "supervisor_id")
    private Long supervisorId;              // BR#26 — Supervisor por categoría

    // ── Cliente ──────────────────────────────────────────────────────────────

    @Column(name = "client_name", length = 200)
    private String clientName;              // BR#50 — OBLIGATORIO

    @Column(name = "client_email", length = 150)
    private String clientEmail;             // BR#53 — para envío de PDF

    @Column(name = "client_phone", length = 20)
    private String clientPhone;             // BR#50 — OBLIGATORIO

    @Column(name = "client_address", columnDefinition = "TEXT")
    private String clientAddress;           // BR#50 — OBLIGATORIO

    // ── Autoridad / Juzgado ──────────────────────────────────────────────────

    @Column(name = "court_name", length = 200)
    private String courtName;               // Nombre del juzgado BR#9

    @Column(name = "court_case_number", length = 100)
    private String courtCaseNumber;         // Número de juzgado BR#9

    @Column(name = "against_party", length = 255)
    private String againstParty;            // BR#7 — Contra quién

    @Enumerated(EnumType.STRING)
    @Column(name = "authority_type", length = 30)
    private AuthorityType authorityType;    // BR#9 — Laboral, Civil, etc.

    @Column(name = "authority_name", length = 255)
    private String authorityName;           // BR#9 — Nombre exacto de la autoridad

    @Column(name = "authority_address", columnDefinition = "TEXT")
    private String authorityAddress;        // BR#10 — Dirección exacta

    // ── Fechas ───────────────────────────────────────────────────────────────

    @Column(name = "due_date")
    private LocalDate dueDate;              // BR#39 — Fecha estimada de procesamiento

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "certified_at")
    private LocalDateTime certifiedAt;      // BR#36 — Solo CERTIFIED

    @Column(name = "certified_by")
    private Long certifiedBy;               // BR#36 — Quién certificó

    // ── Métricas ─────────────────────────────────────────────────────────────

    @Column(name = "estimated_hours")
    private Double estimatedHours;

    @Column(name = "billed_hours")
    @Builder.Default
    private Double billedHours = 0.0;

    @Column(name = "current_version")
    @Builder.Default
    private Integer currentVersion = 1;

    // ── QR / Repositorio ─────────────────────────────────────────────────────

    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;              // BR#16

    @Column(name = "drive_folder_id", length = 255)
    private String driveFolderId;          // BR#22 — Carpeta Google Drive del caso

    @Column(name = "correlation_folder_id", length = 255)
    private String correlationFolderId;    // BR#37 — Carpeta cuando hay correlación

    // ── Colaboración cliente ─────────────────────────────────────────────────

    @Column(name = "enable_client_collab")
    @Builder.Default
    private Boolean enableClientCollab = false;  // BR#38

    @Column(name = "client_collab_description", columnDefinition = "TEXT")
    private String clientCollabDescription;      // BR#38

    // ── Relación entre casos ─────────────────────────────────────────────────

    @Column(name = "parent_case_id")
    private Long parentCaseId;             // BR#17 — Solo admin puede asignar

    // ── Soft delete ──────────────────────────────────────────────────────────

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;       // BR#12 — Solo admin puede eliminar

    @Column(name = "deleted_by")
    private Long deletedBy;

    // ── Relaciones JPA ───────────────────────────────────────────────────────

    @OneToMany(mappedBy = "legalCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("versionNumber DESC")
    private List<CaseVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "legalCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CaseCollaborator> collaborators = new ArrayList<>();

    @OneToMany(mappedBy = "sourceCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CaseRelation> relatedCases = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Helpers ──────────────────────────────────────────────────────────────

    public boolean isActive() {
        return deletedAt == null;
    }

    public boolean isOpen() {
        return status == CaseStatus.CREATED
                || status == CaseStatus.ASSIGNED
                || status == CaseStatus.UPDATED
                || status == CaseStatus.IN_APPROVAL
                || status == CaseStatus.REOPENED;
    }

    public boolean isClosed() {
        return status == CaseStatus.CLOSED
                || status == CaseStatus.COMPLETED
                || status == CaseStatus.FINISHED_UNSUCCESSFULLY
                || status == CaseStatus.FINISHED_SUCCESSFULLY;
    }

    public boolean canBeReopened() {
        // BR#36 — CERTIFIED cases can NEVER be reopened
        return status != CaseStatus.CERTIFIED;
    }

    // ── Enums ────────────────────────────────────────────────────────────────

    public enum CaseStatus {
        NOT_CREATED,            // BR#35 — Estado previo a creación
        CREATED,                // BR#35 — Caso registrado
        ASSIGNED,               // BR#35 — Asignado a usuario
        UPDATED,                // BR#35 — Nueva versión publicada
        IN_APPROVAL,            // BR#35 — Pendiente de aprobación
        CLOSED,                 // BR#35 — Cerrado (no implica terminado)
        COMPLETED,              // BR#35 — Completado con resultado
        FINISHED_UNSUCCESSFULLY,// BR#35 — Terminado sin éxito
        FINISHED_SUCCESSFULLY,  // BR#35 — Terminado con éxito
        REOPENED,               // BR#35 — Reabierto después de cerrar
        CERTIFIED               // BR#36 — Terminal — NUNCA puede reabrirse
    }

    public enum CasePriority {
        LOW, MEDIUM, HIGH, URGENT, CRITICAL
    }

    public enum CaseType {
        CIVIL, CRIMINAL, COMMERCIAL, LABOR, FAMILY, ADMINISTRATIVE,
        CONSTITUTIONAL, INTELLECTUAL_PROPERTY, TAX, REAL_ESTATE, OTHER
    }

    public enum AuthorityType {
        LABORAL, CIVIL, MERCANTIL, SUPREMA_CORTE, FEDERAL, ADMINISTRATIVE, OTHER
    }
}

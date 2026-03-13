package com.legal.cases.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "case_versions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CaseVersion {

    @Id
    @Column(name= "version_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id", nullable = false)
    private Case legalCase;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String content;             // Contenido completo del caso en esta versión

    @Column(name = "change_summary", length = 500)
    private String changeSummary;       // Resumen de cambios respecto a versión anterior

    @Column(name = "created_by", nullable = false)
    private Long createdBy;             // userId

    @Column(name = "created_by_name", length = 100)
    private String createdByName;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private VersionStatus status = VersionStatus.DRAFT;

    @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("createdAt ASC")
    private List<VersionComment> comments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum VersionStatus { DRAFT, REVIEW, APPROVED, REJECTED }
}

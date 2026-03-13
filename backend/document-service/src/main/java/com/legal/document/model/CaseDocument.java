package com.legal.document.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "case_documents", indexes = {
    @Index(name = "idx_doc_case",   columnList = "caseId"),
    @Index(name = "idx_doc_key",    columnList = "documentKey"),
    @Index(name = "idx_doc_status", columnList = "status")
})
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CaseDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Long id;

    @Column(nullable = false)
    private Long caseId;

    @Column(nullable = false, length = 50)
    private String caseNumber;

    @Column(nullable = false, unique = true, length = 50)
    private String documentKey;

    @Column(nullable = false, length = 300)
    private String originalFileName;

    @Column(nullable = false, length = 100)
    private String mimeType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false)
    private String s3Key;

    @Column(nullable = false)
    private String s3BucketName;

    private String googleDriveFileId;
    private String googleDriveFolderId;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Long uploadedByUserId;

    private String uploadedByName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentCategory category;

    @Builder.Default
    private Integer version = 1;

    private Long previousVersionId;

    @Column(length = 64)
    private String checksum;

    @CreationTimestamp
    private LocalDateTime uploadedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public enum DocumentStatus {
        UPLOADING, ACTIVE, ARCHIVED, DELETED, VIRUS_DETECTED
    }

    public enum DocumentCategory {
        CONTRATO, DEMANDA, SENTENCIA, EVIDENCIA, CORRESPONDENCIA,
        FACTURA, PODER_NOTARIAL, IDENTIFICACION, OTRO
    }
}

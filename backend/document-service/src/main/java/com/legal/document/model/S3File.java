package com.legal.document.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "s3_files")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class S3File {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    private S3Folder folder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private CaseDocument document;

    @Column(nullable = false)
    private String s3Key;

    @Column(nullable = false)
    private String fileName;

    private String mimeType;
    private Long sizeBytes;
    private String etag;
    private Integer versionId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

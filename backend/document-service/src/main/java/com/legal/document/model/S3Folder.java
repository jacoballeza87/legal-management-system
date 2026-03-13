package com.legal.document.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "s3_folders")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class S3Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "folder_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket_id", nullable = false)
    private S3Bucket bucket;

    @Column(nullable = false)
    private String folderPath;

    private String displayName;

    private Long caseId;
    private String caseNumber;

    @Builder.Default
    private Long fileCount = 0L;

    @Builder.Default
    private Long totalSizeBytes = 0L;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

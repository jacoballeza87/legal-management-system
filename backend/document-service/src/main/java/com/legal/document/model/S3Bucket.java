package com.legal.document.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "s3_buckets")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class S3Bucket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bucket_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String bucketName;

    @Column(nullable = false, length = 50)
    private String region;

    private String purpose;

    @Builder.Default
    private boolean versioning = false;

    @Builder.Default
    private boolean publicAccess = false;

    @Column(columnDefinition = "TEXT")
    private String lifecyclePolicy;

    @CreationTimestamp
    private LocalDateTime createdAt;
}

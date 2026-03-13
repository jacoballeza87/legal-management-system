package com.legal.document.repository;

import com.legal.document.model.S3Bucket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface S3BucketRepository extends JpaRepository<S3Bucket, Long> {
    Optional<S3Bucket> findByBucketName(String bucketName);
}

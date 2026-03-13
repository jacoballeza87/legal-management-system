package com.legal.document.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiration-minutes:60}")
    private long presignedUrlExpiration;

    /**
     * Sube un archivo a S3.
     * Ruta: cases/{caseNumber}/documents/{uuid}/{sanitizedFilename}
     */
    public String uploadFile(MultipartFile file, Long caseId, String caseNumber) throws Exception {
        String uuid = UUID.randomUUID().toString();
        String s3Key = String.format("cases/%s/documents/%s/%s",
            caseNumber, uuid, sanitize(file.getOriginalFilename()));

        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .contentType(file.getContentType())
            .contentLength(file.getSize())
            .metadata(Map.of(
                "caseId",           caseId.toString(),
                "caseNumber",       caseNumber,
                "originalFilename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed"
            ))
            .build();

        s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        log.info("Archivo subido a S3: bucket={} key={}", bucketName, s3Key);
        return s3Key;
    }

    /**
     * URL pre-firmada para descarga temporal (sin exponer credenciales AWS).
     */
    public String generatePresignedDownloadUrl(String s3Key) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(presignedUrlExpiration))
            .getObjectRequest(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build())
            .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * Soft delete: copia a /deleted/ y elimina el original.
     */
    public void softDeleteFile(String s3Key) {
        String deletedKey = "deleted/" + s3Key;
        try {
            s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(bucketName).sourceKey(s3Key)
                .destinationBucket(bucketName).destinationKey(deletedKey)
                .build());
            s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName).key(s3Key).build());
            log.info("Archivo movido a /deleted: {}", deletedKey);
        } catch (S3Exception e) {
            log.error("Error en soft-delete de {}: {}", s3Key, e.getMessage());
            throw new RuntimeException("Error eliminando archivo de S3", e);
        }
    }

    public HeadObjectResponse getObjectMetadata(String s3Key) {
        return s3Client.headObject(HeadObjectRequest.builder()
            .bucket(bucketName).key(s3Key).build());
    }

    public String getBucketName() {
        return bucketName;
    }

    private String sanitize(String filename) {
        if (filename == null) return "unnamed";
        return filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}

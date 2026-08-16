package com.legal.document.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(name = "gcp.storage.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class GcsService {

    private final Storage storage;

    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    @Value("${gcp.storage.signed-url-expiration-minutes:60}")
    private long signedUrlExpirationMinutes;

    public String uploadFile(MultipartFile file, Long caseId, String caseNumber) throws Exception {
        String uuid = UUID.randomUUID().toString();
        String objectKey = String.format("cases/%s/documents/%s/%s",
            caseNumber, uuid, sanitize(file.getOriginalFilename()));

        BlobId blobId = BlobId.of(bucketName, objectKey);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
            .setContentType(file.getContentType())
            .setMetadata(Map.of(
                "caseId", caseId.toString(),
                "caseNumber", caseNumber,
                "originalFilename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed"
            ))
            .build();

        storage.create(blobInfo, file.getBytes());
        log.info("Archivo subido a Cloud Storage: bucket={} key={}", bucketName, objectKey);
        return objectKey;
    }

    public String generatePresignedDownloadUrl(String objectKey) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectKey)).build();
        return storage.signUrl(blobInfo, signedUrlExpirationMinutes, TimeUnit.MINUTES,
            Storage.SignUrlOption.withV4Signature()).toString();
    }

    public void softDeleteFile(String objectKey) {
        String deletedKey = "deleted/" + objectKey;
        try {
            BlobId sourceId = BlobId.of(bucketName, objectKey);
            BlobId targetId = BlobId.of(bucketName, deletedKey);

            Storage.CopyRequest copyRequest = Storage.CopyRequest.newBuilder()
                .setSource(sourceId)
                .setTarget(targetId)
                .build();
            storage.copy(copyRequest).getResult();
            storage.delete(sourceId);

            log.info("Archivo movido a /deleted: {}", deletedKey);
        } catch (StorageException e) {
            log.error("Error en soft-delete de {}: {}", objectKey, e.getMessage());
            throw new RuntimeException("Error eliminando archivo de Cloud Storage", e);
        }
    }

    public Blob getObjectMetadata(String objectKey) {
        return storage.get(BlobId.of(bucketName, objectKey));
    }

    public String getBucketName() {
        return bucketName;
    }

    private String sanitize(String filename) {
        if (filename == null) return "unnamed";
        return filename.replaceAll("[^a-zA-Z0-9._\\-]", "_");
    }
}

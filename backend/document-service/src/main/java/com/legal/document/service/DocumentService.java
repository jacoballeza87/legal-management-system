package com.legal.document.service;

import com.legal.document.dto.DocumentResponse;
import com.legal.document.dto.DocumentUploadRequest;
import com.legal.document.model.CaseDocument;
import com.legal.document.model.CaseDocument.*;
import com.legal.document.repository.CaseDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final CaseDocumentRepository documentRepo;
    private final FileValidationService validationService;
    private final GcsService gcsService;

    // ── Upload ─────────────────────────────────────────────────────────────────

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, DocumentUploadRequest request) throws Exception {
        // 1. Validate
        validationService.validate(file);

        // 2. Detect real MIME type
        String mimeType = validationService.detectMimeType(file);

        // 3. Checksum
        byte[] bytes = file.getBytes();
        String checksum = calculateChecksum(bytes);

        // 4. Save to Cloud Storage
        String objectKey = gcsService.uploadFile(file, request.getCaseId(), request.getCaseNumber());

        // 5. Version
        int version = request.getPreviousVersionId() != null
            ? documentRepo.getNextVersion(request.getCaseId(), file.getOriginalFilename())
            : 1;

        // 6. Persist
        CaseDocument document = CaseDocument.builder()
            .caseId(request.getCaseId())
            .caseNumber(request.getCaseNumber())
            .documentKey(UUID.randomUUID().toString())
            .originalFileName(file.getOriginalFilename())
            .mimeType(mimeType)
            .fileSize(file.getSize())
            .s3Key(objectKey)                       // reusing s3Key field to store GCS object key
            .s3BucketName(gcsService.getBucketName())
            .googleDriveFileId(null)
            .googleDriveFolderId(null)
            .description(request.getDescription())
            .uploadedByUserId(request.getUploadedByUserId())
            .uploadedByName(request.getUploadedByName())
            .category(request.getCategory())
            .version(version)
            .previousVersionId(request.getPreviousVersionId())
            .checksum(checksum)
            .status(DocumentStatus.ACTIVE)
            .build();

        document = documentRepo.save(document);
        log.info("Document saved id={} key={} case={}", document.getId(),
            document.getDocumentKey(), request.getCaseNumber());

        return mapToResponse(document);
    }

    // ── Read ───────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByCase(Long caseId) {
        return documentRepo.findByCaseIdAndStatusNot(caseId, DocumentStatus.DELETED)
            .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long documentId) {
        CaseDocument doc = documentRepo.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        return mapToResponse(doc);
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteDocument(Long documentId, Long requestingUserId) {
        CaseDocument doc = documentRepo.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        try {
            gcsService.softDeleteFile(doc.getS3Key());
        } catch (Exception e) {
            log.warn("Could not soft-delete file in Cloud Storage: {}", e.getMessage());
        }

        doc.setStatus(DocumentStatus.DELETED);
        doc.setDeletedAt(LocalDateTime.now());
        documentRepo.save(doc);
        log.info("Document id={} deleted by userId={}", documentId, requestingUserId);
    }

    // ── Stats ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getCaseDocumentStats(Long caseId) {
        long active  = documentRepo.countByCaseIdAndStatus(caseId, DocumentStatus.ACTIVE);
        long deleted = documentRepo.countByCaseIdAndStatus(caseId, DocumentStatus.DELETED);
        Long totalBytes = documentRepo.getTotalSizeByCaseId(caseId);
        return Map.of(
            "activeDocuments",    active,
            "deletedDocuments",   deleted,
            "totalDocuments",     active + deleted,
            "totalSize",          totalBytes != null ? totalBytes : 0L,
            "totalSizeFormatted", formatFileSize(totalBytes != null ? totalBytes : 0L)
        );
    }

    // ── Checksum ───────────────────────────────────────────────────────────────

    private String calculateChecksum(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.warn("Could not calculate checksum: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }

    // ── Mapping ────────────────────────────────────────────────────────────────

    private DocumentResponse mapToResponse(CaseDocument doc) {
        String downloadUrl = null;
        if (doc.getStatus() == DocumentStatus.ACTIVE && doc.getS3Key() != null) {
            downloadUrl = gcsService.generatePresignedDownloadUrl(doc.getS3Key());
        }

        return DocumentResponse.builder()
            .id(doc.getId())
            .caseId(doc.getCaseId())
            .caseNumber(doc.getCaseNumber())
            .documentKey(doc.getDocumentKey())
            .originalFileName(doc.getOriginalFileName())
            .mimeType(doc.getMimeType())
            .fileSize(doc.getFileSize())
            .fileSizeFormatted(formatFileSize(doc.getFileSize()))
            .description(doc.getDescription())
            .uploadedByUserId(doc.getUploadedByUserId())
            .uploadedByName(doc.getUploadedByName())
            .status(doc.getStatus())
            .category(doc.getCategory())
            .version(doc.getVersion())
            .previousVersionId(doc.getPreviousVersionId())
            .downloadUrl(downloadUrl)
            .googleDriveUrl(null)
            .uploadedAt(doc.getUploadedAt())
            .updatedAt(doc.getUpdatedAt())
            .build();
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes == 0) return "0 B";
        if (bytes < 1_024)             return bytes + " B";
        if (bytes < 1_048_576)         return String.format("%.1f KB", bytes / 1_024.0);
        if (bytes < 1_073_741_824)     return String.format("%.1f MB", bytes / 1_048_576.0);
        return                                String.format("%.1f GB", bytes / 1_073_741_824.0);
    }
}

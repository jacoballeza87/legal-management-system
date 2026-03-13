package com.legal.document.service;

import com.legal.document.dto.DocumentResponse;
import com.legal.document.dto.DocumentUploadRequest;
import com.legal.document.model.CaseDocument;
import com.legal.document.model.CaseDocument.*;
import com.legal.document.repository.CaseDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final CaseDocumentRepository documentRepo;
    private final S3Service s3Service;
    private final GoogleDriveService driveService;
    private final FileValidationService validationService;

    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, DocumentUploadRequest request) throws Exception {
        // 1. Validar
        validationService.validate(file);

        // 2. Detectar MIME real
        String mimeType = validationService.detectMimeType(file);

        // 3. Checksum
        byte[] bytes = file.getBytes();
        String checksum = validationService.calculateChecksum(bytes);

        // 4. Subir a S3
        String s3Key = s3Service.uploadFile(file, request.getCaseId(), request.getCaseNumber());

        // 5. Google Drive (opcional, no bloquea si falla)
        String driveFileId = null;
        String driveFolderId = null;
        try {
            List<String> existingFolderIds = documentRepo.findDriveFolderIds(request.getCaseId());
            if (!existingFolderIds.isEmpty()) {
                driveFolderId = existingFolderIds.get(0);
            } else {
                driveFolderId = driveService.createCaseFolder(request.getCaseNumber(), null);
            }
            if (driveFolderId != null) {
                driveFileId = driveService.uploadFileToDrive(file, driveFolderId);
            }
        } catch (Exception e) {
            log.warn("Error Google Drive (no crítico): {}", e.getMessage());
        }

        // 6. Versión
        int version = request.getPreviousVersionId() != null
            ? documentRepo.getNextVersion(request.getCaseId(), file.getOriginalFilename())
            : 1;

        // 7. Persistir
        CaseDocument document = CaseDocument.builder()
            .caseId(request.getCaseId())
            .caseNumber(request.getCaseNumber())
            .documentKey(UUID.randomUUID().toString())
            .originalFileName(file.getOriginalFilename())
            .mimeType(mimeType)
            .fileSize(file.getSize())
            .s3Key(s3Key)
            .s3BucketName(s3Service.getBucketName())
            .googleDriveFileId(driveFileId)
            .googleDriveFolderId(driveFolderId)
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
        log.info("Documento guardado id={} key={} caso={}", document.getId(),
            document.getDocumentKey(), request.getCaseNumber());

        return mapToResponse(document);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocumentsByCase(Long caseId) {
        return documentRepo.findByCaseIdAndStatusNot(caseId, DocumentStatus.DELETED)
            .stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long documentId) {
        CaseDocument doc = documentRepo.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Documento no encontrado: " + documentId));
        return mapToResponse(doc);
    }

    @Transactional
    public void deleteDocument(Long documentId, Long requestingUserId) {
        CaseDocument doc = documentRepo.findById(documentId)
            .orElseThrow(() -> new RuntimeException("Documento no encontrado: " + documentId));

        s3Service.softDeleteFile(doc.getS3Key());

        if (doc.getGoogleDriveFileId() != null) {
            try { driveService.deleteFile(doc.getGoogleDriveFileId()); }
            catch (Exception e) { log.warn("Error eliminando de Drive: {}", e.getMessage()); }
        }

        doc.setStatus(DocumentStatus.DELETED);
        doc.setDeletedAt(LocalDateTime.now());
        documentRepo.save(doc);
        log.info("Documento id={} eliminado por userId={}", documentId, requestingUserId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCaseDocumentStats(Long caseId) {
        long active  = documentRepo.countByCaseIdAndStatus(caseId, DocumentStatus.ACTIVE);
        long deleted = documentRepo.countByCaseIdAndStatus(caseId, DocumentStatus.DELETED);
        Long totalBytes = documentRepo.getTotalSizeByCaseId(caseId);
        return Map.of(
            "activeDocuments",   active,
            "deletedDocuments",  deleted,
            "totalDocuments",    active + deleted,
            "totalSize",         totalBytes != null ? totalBytes : 0L,
            "totalSizeFormatted", formatFileSize(totalBytes != null ? totalBytes : 0L)
        );
    }

    // ── Mapping ────────────────────────────────────────────────────────────────

    private DocumentResponse mapToResponse(CaseDocument doc) {
        String downloadUrl = null;
        if (doc.getStatus() == DocumentStatus.ACTIVE) {
            try { downloadUrl = s3Service.generatePresignedDownloadUrl(doc.getS3Key()); }
            catch (Exception e) { log.warn("Error generando presigned URL: {}", e.getMessage()); }
        }

        String driveUrl = null;
        if (doc.getGoogleDriveFileId() != null) {
            try { driveUrl = driveService.getFileViewLink(doc.getGoogleDriveFileId()); }
            catch (Exception e) { log.debug("No se pudo obtener URL Drive: {}", e.getMessage()); }
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
            .googleDriveUrl(driveUrl)
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

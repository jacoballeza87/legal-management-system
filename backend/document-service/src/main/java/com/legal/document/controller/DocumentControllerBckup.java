package com.legal.document.controller;

import com.legal.document.dto.DocumentResponse;
import com.legal.document.dto.DocumentUploadRequest;
import com.legal.document.model.CaseDocument.DocumentCategory;
import com.legal.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /** Subir documento — LAWYER, ADMIN, SUPER_ADMIN */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_LAWYER','ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<DocumentResponse> upload(
            @RequestPart("file")               MultipartFile file,
            @RequestPart("caseId")             String caseId,
            @RequestPart("caseNumber")         String caseNumber,
            @RequestPart("uploadedByUserId")   String uploadedByUserId,
            @RequestPart(value = "uploadedByName",  required = false) String uploadedByName,
            @RequestPart(value = "description", required = false)     String description,
            @RequestPart("category")           String category) throws Exception {

        DocumentUploadRequest request = DocumentUploadRequest.builder()
            .caseId(Long.parseLong(caseId))
            .caseNumber(caseNumber)
            .uploadedByUserId(Long.parseLong(uploadedByUserId))
            .uploadedByName(uploadedByName)
            .description(description)
            .category(DocumentCategory.valueOf(category))
            .build();

        return ResponseEntity.ok(documentService.uploadDocument(file, request));
    }

    /** Documentos de un caso */
    @GetMapping("/case/{caseId}")
    @PreAuthorize("hasAnyRole('ROLE_LAWYER','ROLE_ACCOUNTANT','ROLE_ADMIN','ROLE_SUPER_ADMIN','ROLE_VIEWER')")
    public ResponseEntity<List<DocumentResponse>> getByCase(@PathVariable Long caseId) {
        return ResponseEntity.ok(documentService.getDocumentsByCase(caseId));
    }

    /** Documento por ID */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocument(id));
    }

    /** Eliminar (soft-delete) */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_LAWYER','ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, String>> deleteDocument(
            @PathVariable Long id,
            @RequestParam Long requestingUserId) {
        documentService.deleteDocument(id, requestingUserId);
        return ResponseEntity.ok(Map.of("message", "Documento eliminado exitosamente"));
    }

    /** Estadísticas de documentos de un caso */
    @GetMapping("/case/{caseId}/stats")
    public ResponseEntity<Map<String, Object>> getCaseStats(@PathVariable Long caseId) {
        return ResponseEntity.ok(documentService.getCaseDocumentStats(caseId));
    }
}

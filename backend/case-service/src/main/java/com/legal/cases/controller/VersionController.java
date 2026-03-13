package com.legal.cases.controller;

import com.legal.cases.dto.*;
import com.legal.cases.model.CaseVersion;
import com.legal.cases.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cases/{caseId}/versions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Versions", description = "Gestión de versiones de casos")
public class VersionController {

    private final VersionService versionService;

    @GetMapping
    @Operation(summary = "Listar versiones de un caso")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<VersionDTO>> getVersions(@PathVariable Long caseId) {
        return ResponseEntity.ok(versionService.getVersionsByCase(caseId));
    }

    @GetMapping("/{versionId}")
    @Operation(summary = "Obtener versión por ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VersionDTO> getVersion(@PathVariable Long caseId, @PathVariable Long versionId) {
        return ResponseEntity.ok(versionService.getVersionById(versionId));
    }

    @PostMapping
    @Operation(summary = "Crear nueva versión del caso")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','LAWYER')")
    public ResponseEntity<VersionDTO> createVersion(
            @PathVariable Long caseId,
            @Valid @RequestBody CreateVersionRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String userName = (String) httpRequest.getAttribute("userEmail");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(versionService.createVersion(caseId, request, userId, userName));
    }

    @PatchMapping("/{versionId}/status")
    @Operation(summary = "Cambiar estado de una versión (DRAFT → REVIEW → APPROVED)")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<VersionDTO> updateStatus(
            @PathVariable Long caseId,
            @PathVariable Long versionId,
            @RequestParam CaseVersion.VersionStatus status) {
        return ResponseEntity.ok(versionService.updateVersionStatus(versionId, status));
    }

    // ─── Comentarios ─────────────────────────────────────────────────────────────

    @GetMapping("/{versionId}/comments")
    @Operation(summary = "Listar comentarios de una versión")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CommentDTO>> getComments(
            @PathVariable Long caseId, @PathVariable Long versionId) {
        return ResponseEntity.ok(versionService.getComments(versionId));
    }

    @PostMapping("/{versionId}/comments")
    @Operation(summary = "Agregar comentario a una versión")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentDTO> addComment(
            @PathVariable Long caseId,
            @PathVariable Long versionId,
            @Valid @RequestBody CreateCommentRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        String userName = (String) httpRequest.getAttribute("userEmail");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(versionService.addComment(versionId, request, userId, userName));
    }

    @PutMapping("/{versionId}/comments/{commentId}")
    @Operation(summary = "Editar comentario (solo el autor)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable Long caseId,
            @PathVariable Long versionId,
            @PathVariable Long commentId,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(versionService.updateComment(commentId, body.get("content"), userId));
    }

    @DeleteMapping("/{versionId}/comments/{commentId}")
    @Operation(summary = "Eliminar comentario (solo el autor)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> deleteComment(
            @PathVariable Long caseId,
            @PathVariable Long versionId,
            @PathVariable Long commentId,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        versionService.deleteComment(commentId, userId);
        return ResponseEntity.ok(Map.of("message", "Comentario eliminado"));
    }
}

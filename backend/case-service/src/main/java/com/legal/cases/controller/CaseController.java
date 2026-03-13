package com.legal.cases.controller;

import com.legal.cases.dto.*;
import com.legal.cases.model.Case;
import com.legal.cases.service.CaseService;
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
@RequestMapping("/api/v1/cases")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cases", description = "Gestión de casos legales")
public class CaseController {

    private final CaseService caseService;

    // ─── Listado y búsqueda ───────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Listar todos los casos (admin) o los propios (abogado)")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<CaseDTO>> getCases(
            @ModelAttribute CaseFilterRequest filter,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("userRole");

        if ("SUPER_ADMIN".equals(role) || "ADMIN".equals(role)) {
            return ResponseEntity.ok(caseService.getAllCases(filter));
        }
        return ResponseEntity.ok(caseService.getCasesByOwner(userId, filter));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener caso por ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CaseDTO> getCaseById(@PathVariable Long id) {
        return ResponseEntity.ok(caseService.getCaseById(id));
    }

    @GetMapping("/number/{caseNumber}")
    @Operation(summary = "Obtener caso por número de expediente")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CaseDTO> getCaseByCaseNumber(@PathVariable String caseNumber) {
        return ResponseEntity.ok(caseService.getCaseByCaseNumber(caseNumber));
    }

    @GetMapping("/my-collaborations")
    @Operation(summary = "Casos donde el usuario es colaborador")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<CaseDTO>> getCollaborations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(caseService.getCasesWhereCollaborator(userId, page, size));
    }

    @GetMapping("/stats")
    @Operation(summary = "Estadísticas generales de casos")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<CaseStatsDTO> getStats() {
        return ResponseEntity.ok(caseService.getStats());
    }

    // ─── CRUD ─────────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Crear nuevo caso")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','LAWYER')")
    public ResponseEntity<CaseDTO> createCase(
            @Valid @RequestBody CreateCaseRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED).body(caseService.createCase(request, userId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar caso")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','LAWYER')")
    public ResponseEntity<CaseDTO> updateCase(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCaseRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(caseService.updateCase(id, request, userId));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar estado del caso")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','LAWYER')")
    public ResponseEntity<CaseDTO> changeStatus(
            @PathVariable Long id,
            @RequestParam Case.CaseStatus status,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(caseService.changeStatus(id, status, userId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar caso")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN')")
    public ResponseEntity<Map<String, String>> deleteCase(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        caseService.deleteCase(id, userId);
        return ResponseEntity.ok(Map.of("message", "Caso eliminado exitosamente"));
    }

    // ─── Colaboradores ────────────────────────────────────────────────────────────

    @GetMapping("/{id}/collaborators")
    @Operation(summary = "Listar colaboradores del caso")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CollaboratorDTO>> getCollaborators(@PathVariable Long id) {
        return ResponseEntity.ok(caseService.getCollaborators(id));
    }

    @PostMapping("/{id}/collaborators")
    @Operation(summary = "Agregar colaborador al caso")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','LAWYER')")
    public ResponseEntity<CollaboratorDTO> addCollaborator(
            @PathVariable Long id,
            @Valid @RequestBody AddCollaboratorRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        return ResponseEntity.status(HttpStatus.CREATED).body(caseService.addCollaborator(id, request, userId));
    }

    @DeleteMapping("/{id}/collaborators/{userId}")
    @Operation(summary = "Eliminar colaborador del caso")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','LAWYER')")
    public ResponseEntity<Map<String, String>> removeCollaborator(
            @PathVariable Long id,
            @PathVariable Long userId) {
        caseService.removeCollaborator(id, userId);
        return ResponseEntity.ok(Map.of("message", "Colaborador eliminado"));
    }

    // ─── QR Code ─────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/qr/regenerate")
    @Operation(summary = "Regenerar QR code del caso")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','LAWYER')")
    public ResponseEntity<Map<String, String>> regenerateQR(@PathVariable Long id) {
        String qrUrl = caseService.regenerateQR(id);
        return ResponseEntity.ok(Map.of("qrCodeUrl", qrUrl));
    }
}

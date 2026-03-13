package com.legal.cases.service;

import com.legal.cases.dto.*;
import com.legal.cases.exception.CaseNotFoundException;
import com.legal.cases.kafka.CaseEventProducer;
import com.legal.cases.mapper.CaseMapper;
import com.legal.cases.model.*;
import com.legal.cases.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {

    private final CaseRepository caseRepository;
    private final CaseVersionRepository versionRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final CaseMapper caseMapper;
    private final QRCodeService qrCodeService;
    private final CaseEventProducer eventProducer;

    // ─── Consultas ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PageResponse<CaseDTO> getAllCases(CaseFilterRequest filter) {
        Pageable pageable = buildPageable(filter);
        Page<CaseDTO> page;

        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            page = caseRepository.searchCases(filter.getSearch(), pageable).map(caseMapper::toDTO);
        } else if (filter.getStatus() != null) {
            page = caseRepository.findByStatus(filter.getStatus(), pageable).map(caseMapper::toDTO);
        } else {
            page = caseRepository.findAll(pageable).map(caseMapper::toDTO);
        }
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<CaseDTO> getCasesByOwner(Long ownerId, CaseFilterRequest filter) {
        Pageable pageable = buildPageable(filter);
        Page<CaseDTO> page;

        if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
            page = caseRepository.searchCasesByOwner(filter.getSearch(), ownerId, pageable).map(caseMapper::toDTO);
        } else {
            page = caseRepository.findByOwnerId(ownerId, pageable).map(caseMapper::toDTO);
        }
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public CaseDTO getCaseById(Long id) {
        return caseRepository.findById(id)
                .map(caseMapper::toDTO)
                .orElseThrow(() -> new CaseNotFoundException("Caso no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public CaseDTO getCaseByCaseNumber(String caseNumber) {
        return caseRepository.findByCaseNumber(caseNumber)
                .map(caseMapper::toDTO)
                .orElseThrow(() -> new CaseNotFoundException("Caso no encontrado: " + caseNumber));
    }

    @Transactional(readOnly = true)
    public PageResponse<CaseDTO> getCasesWhereCollaborator(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return PageResponse.from(
                caseRepository.findCasesWhereUserIsCollaborator(userId, pageable).map(caseMapper::toDTO));
    }

    // ─── Creación ────────────────────────────────────────────────────────────────

    @Transactional
    public CaseDTO createCase(CreateCaseRequest request, Long ownerId) {
        Case legalCase = caseMapper.toEntity(request);
        legalCase.setOwnerId(ownerId);
        legalCase.setCaseNumber(generateCaseNumber());

        // Guardar primero para obtener ID
        legalCase = caseRepository.save(legalCase);

        // Generar QR con el ID ya asignado
        String qrUrl = qrCodeService.generateQRCodeBase64(legalCase.getCaseNumber(), legalCase.getId());
        legalCase.setQrCodeUrl(qrUrl);

        // Crear versión inicial automáticamente
        CaseVersion initialVersion = CaseVersion.builder()
                .legalCase(legalCase)
                .versionNumber(1)
                .title("Versión inicial")
                .description("Creación del caso")
                .content(request.getDescription() != null ? request.getDescription() : "")
                .changeSummary("Caso creado")
                .createdBy(ownerId)
                .status(CaseVersion.VersionStatus.APPROVED)
                .build();
        versionRepository.save(initialVersion);

        legalCase = caseRepository.save(legalCase);
        log.info("Caso creado: {} por userId: {}", legalCase.getCaseNumber(), ownerId);

        eventProducer.publishCaseCreated(legalCase, ownerId);
        return caseMapper.toDTO(legalCase);
    }

    // ─── Actualización ───────────────────────────────────────────────────────────

    @Transactional
    public CaseDTO updateCase(Long id, UpdateCaseRequest request, Long userId) {
        Case legalCase = caseRepository.findById(id)
                .orElseThrow(() -> new CaseNotFoundException("Caso no encontrado: " + id));

        Case.CaseStatus previousStatus = legalCase.getStatus();
        caseMapper.updateEntity(request, legalCase);

        // Si el estado cambió a CLOSED o CANCELLED, registrar fecha de cierre
        if (request.getStatus() != null
                && request.getStatus() != previousStatus
                && (request.getStatus() == Case.CaseStatus.CLOSED
                    || request.getStatus() == Case.CaseStatus.FINISHED_UNSUCCESSFULLY)) {
            legalCase.setClosedAt(LocalDateTime.now());
        }

        Case saved = caseRepository.save(legalCase);

        if (request.getStatus() != null && request.getStatus() != previousStatus) {
            eventProducer.publishStatusChanged(saved, previousStatus, userId);
        } else {
            eventProducer.publishCaseUpdated(saved, userId);
        }

        log.info("Caso {} actualizado por userId: {}", saved.getCaseNumber(), userId);
        return caseMapper.toDTO(saved);
    }

    @Transactional
    public CaseDTO changeStatus(Long id, Case.CaseStatus newStatus, Long userId) {
        Case legalCase = caseRepository.findById(id)
                .orElseThrow(() -> new CaseNotFoundException("Caso no encontrado: " + id));

        Case.CaseStatus previousStatus = legalCase.getStatus();
        if (previousStatus == newStatus) return caseMapper.toDTO(legalCase);

        legalCase.setStatus(newStatus);
        if (newStatus == Case.CaseStatus.CLOSED || newStatus == Case.CaseStatus.FINISHED_UNSUCCESSFULLY) {
            legalCase.setClosedAt(LocalDateTime.now());
        }

        Case saved = caseRepository.save(legalCase);
        eventProducer.publishStatusChanged(saved, previousStatus, userId);
        log.info("Estado del caso {} cambiado: {} → {}", saved.getCaseNumber(), previousStatus, newStatus);
        return caseMapper.toDTO(saved);
    }

    // ─── Colaboradores ────────────────────────────────────────────────────────────

    @Transactional
    public CollaboratorDTO addCollaborator(Long caseId, AddCollaboratorRequest request, Long addedBy) {
        Case legalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new CaseNotFoundException("Caso no encontrado: " + caseId));

        if (collaboratorRepository.existsByLegalCaseIdAndUserId(caseId, request.getUserId())) {
            throw new IllegalArgumentException("El usuario ya es colaborador de este caso");
        }

        CaseCollaborator collaborator = CaseCollaborator.builder()
                .legalCase(legalCase)
                .userId(request.getUserId())
                .userName(request.getUserName())
                .userEmail(request.getUserEmail())
                .role(request.getRole())
                .addedBy(addedBy)
                .build();

        CaseCollaborator saved = collaboratorRepository.save(collaborator);
        eventProducer.publishCollaboratorAdded(legalCase, request.getUserId(), addedBy);
        return caseMapper.toDTO(saved);
    }

    @Transactional
    public void removeCollaborator(Long caseId, Long userId) {
        if (!caseRepository.existsById(caseId)) throw new CaseNotFoundException("Caso no encontrado: " + caseId);
        collaboratorRepository.deleteByCaseIdAndUserId(caseId, userId);
    }

    @Transactional(readOnly = true)
    public List<CollaboratorDTO> getCollaborators(Long caseId) {
        return collaboratorRepository.findByLegalCaseId(caseId)
                .stream().map(caseMapper::toDTO).collect(Collectors.toList());
    }

    // ─── QR Code ─────────────────────────────────────────────────────────────────

    @Transactional
    public String regenerateQR(Long caseId) {
        Case legalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new CaseNotFoundException("Caso no encontrado: " + caseId));
        String qrUrl = qrCodeService.generateQRCodeBase64(legalCase.getCaseNumber(), caseId);
        legalCase.setQrCodeUrl(qrUrl);
        caseRepository.save(legalCase);
        return qrUrl;
    }

    // ─── Estadísticas ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CaseStatsDTO getStats() {
        Map<String, Long> byStatus = toMap(caseRepository.countByStatus());
        Map<String, Long> byPriority = toMap(caseRepository.countByPriority());
        Map<String, Long> byType = toMap(caseRepository.countByType());

        long overdue = caseRepository.findOverdueCases(LocalDate.now()).size();
        long dueSoon = caseRepository.findCasesDueSoon(LocalDate.now(), LocalDate.now().plusDays(7)).size();

        return CaseStatsDTO.builder()
                .total(caseRepository.count())
                .byStatus(byStatus).byPriority(byPriority).byType(byType)
                .overdue(overdue).dueSoon(dueSoon)
                .build();
    }

    // ─── Eliminación ─────────────────────────────────────────────────────────────

    @Transactional
    public void deleteCase(Long id, Long userId) {
        Case legalCase = caseRepository.findById(id)
                .orElseThrow(() -> new CaseNotFoundException("Caso no encontrado: " + id));
        String caseNumber = legalCase.getCaseNumber();
        caseRepository.delete(legalCase);
        eventProducer.publishCaseDeleted(id, caseNumber, userId);
        log.info("Caso {} eliminado por userId: {}", caseNumber, userId);
    }

    // ─── Helpers privados ────────────────────────────────────────────────────────

    private String generateCaseNumber() {
        int year = Year.now().getValue();
        long count = caseRepository.count() + 1;
        String candidate;
        do {
            candidate = String.format("CASE-%d-%05d", year, count++);
        } while (caseRepository.existsByCaseNumber(candidate));
        return candidate;
    }

    private Pageable buildPageable(CaseFilterRequest f) {
        Sort sort = f.getDirection().equalsIgnoreCase("desc")
                ? Sort.by(f.getSortBy()).descending()
                : Sort.by(f.getSortBy()).ascending();
        return PageRequest.of(f.getPage(), Math.min(f.getSize(), 100), sort);
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : rows) {
            map.put(row[0].toString(), (Long) row[1]);
        }
        return map;
    }
}

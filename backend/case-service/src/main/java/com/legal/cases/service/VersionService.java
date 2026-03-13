package com.legal.cases.service;

import com.legal.cases.dto.*;
import com.legal.cases.exception.CaseNotFoundException;
import com.legal.cases.mapper.CaseMapper;
import com.legal.cases.model.*;
import com.legal.cases.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VersionService {

    private final CaseVersionRepository versionRepository;
    private final CaseRepository caseRepository;
    private final CommentRepository commentRepository;
    private final CaseMapper caseMapper;

    @Transactional(readOnly = true)
    public List<VersionDTO> getVersionsByCase(Long caseId) {
        if (!caseRepository.existsById(caseId)) throw new CaseNotFoundException("Caso no encontrado: " + caseId);
        return versionRepository.findByLegalCaseIdOrderByVersionNumberDesc(caseId)
                .stream().map(caseMapper::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VersionDTO getVersionById(Long versionId) {
        return versionRepository.findById(versionId)
                .map(caseMapper::toDTO)
                .orElseThrow(() -> new IllegalArgumentException("Versión no encontrada: " + versionId));
    }

    @Transactional
    public VersionDTO createVersion(Long caseId, CreateVersionRequest request, Long userId, String userName) {
        Case legalCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new CaseNotFoundException("Caso no encontrado: " + caseId));

        if (legalCase.isClosed()) {
            throw new IllegalStateException("No se pueden agregar versiones a un caso cerrado");
        }

        int nextVersion = versionRepository.findMaxVersionNumber(caseId).orElse(0) + 1;

        CaseVersion version = CaseVersion.builder()
                .legalCase(legalCase)
                .versionNumber(nextVersion)
                .title(request.getTitle())
                .description(request.getDescription())
                .content(request.getContent())
                .changeSummary(request.getChangeSummary())
                .createdBy(userId)
                .createdByName(userName)
                .status(CaseVersion.VersionStatus.DRAFT)
                .build();

        CaseVersion saved = versionRepository.save(version);
        caseRepository.updateCurrentVersion(caseId, nextVersion);

        log.info("Versión {} creada para caso {}", nextVersion, legalCase.getCaseNumber());
        return caseMapper.toDTO(saved);
    }

    @Transactional
    public VersionDTO updateVersionStatus(Long versionId, CaseVersion.VersionStatus newStatus) {
        CaseVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Versión no encontrada: " + versionId));
        version.setStatus(newStatus);
        return caseMapper.toDTO(versionRepository.save(version));
    }

    // ─── Comentarios ─────────────────────────────────────────────────────────────

    @Transactional
    public CommentDTO addComment(Long versionId, CreateCommentRequest request, Long authorId, String authorName) {
        CaseVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Versión no encontrada: " + versionId));

        VersionComment comment = VersionComment.builder()
                .version(version)
                .content(request.getContent())
                .authorId(authorId)
                .authorName(authorName)
                .build();

        return caseMapper.toDTO(commentRepository.save(comment));
    }

    @Transactional
    public CommentDTO updateComment(Long commentId, String newContent, Long userId) {
        VersionComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comentario no encontrado: " + commentId));

        if (!comment.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("Solo el autor puede editar el comentario");
        }

        comment.setContent(newContent);
        comment.setIsEdited(true);
        return caseMapper.toDTO(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        VersionComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comentario no encontrado: " + commentId));
        if (!comment.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException("Solo el autor puede eliminar el comentario");
        }
        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentDTO> getComments(Long versionId) {
        return commentRepository.findByVersionIdOrderByCreatedAtAsc(versionId)
                .stream().map(caseMapper::toDTO).collect(Collectors.toList());
    }
}

package com.legal.document.repository;

import com.legal.document.model.CaseDocument;
import com.legal.document.model.CaseDocument.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CaseDocumentRepository extends JpaRepository<CaseDocument, Long> {

    List<CaseDocument> findByCaseIdAndStatusNot(Long caseId, DocumentStatus status);

    List<CaseDocument> findByCaseIdAndCategory(Long caseId, DocumentCategory category);

    Optional<CaseDocument> findByDocumentKey(String documentKey);

    @Query("SELECT d.googleDriveFolderId FROM CaseDocument d WHERE d.caseId = :caseId " +
           "AND d.googleDriveFolderId IS NOT NULL ORDER BY d.uploadedAt DESC")
    List<String> findDriveFolderIds(@Param("caseId") Long caseId);

    @Query("SELECT COALESCE(MAX(d.version), 0) + 1 FROM CaseDocument d " +
           "WHERE d.caseId = :caseId AND d.originalFileName = :filename")
    int getNextVersion(@Param("caseId") Long caseId, @Param("filename") String filename);

    long countByCaseIdAndStatus(Long caseId, DocumentStatus status);

    @Query("SELECT COALESCE(SUM(d.fileSize), 0) FROM CaseDocument d WHERE d.caseId = :caseId " +
           "AND d.status != 'DELETED'")
    Long getTotalSizeByCaseId(@Param("caseId") Long caseId);
}

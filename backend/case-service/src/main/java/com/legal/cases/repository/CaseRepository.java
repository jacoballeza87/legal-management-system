package com.legal.cases.repository;

import com.legal.cases.model.Case;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CaseRepository extends JpaRepository<Case, Long>, JpaSpecificationExecutor<Case> {

    Optional<Case> findByCaseNumber(String caseNumber);
    boolean existsByCaseNumber(String caseNumber);

    Page<Case> findByOwnerId(Long ownerId, Pageable pageable);
    Page<Case> findByStatus(Case.CaseStatus status, Pageable pageable);
    Page<Case> findByOwnerIdAndStatus(Long ownerId, Case.CaseStatus status, Pageable pageable);

    @Query("SELECT c FROM Case c WHERE " +
           "LOWER(c.title) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(c.caseNumber) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(c.clientName) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(c.courtCaseNumber) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Case> searchCases(@Param("q") String query, Pageable pageable);

    @Query("SELECT c FROM Case c WHERE c.ownerId = :ownerId AND (" +
           "LOWER(c.title) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(c.caseNumber) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Case> searchCasesByOwner(@Param("q") String query, @Param("ownerId") Long ownerId, Pageable pageable);

    // Casos vencidos
    @Query("SELECT c FROM Case c WHERE c.dueDate < :today AND c.status NOT IN ('CLOSED','ARCHIVED','CANCELLED')")
    List<Case> findOverdueCases(@Param("today") LocalDate today);

    // Casos que vencen pronto
    @Query("SELECT c FROM Case c WHERE c.dueDate BETWEEN :today AND :limit AND c.status NOT IN ('CLOSED','ARCHIVED','CANCELLED')")
    List<Case> findCasesDueSoon(@Param("today") LocalDate today, @Param("limit") LocalDate limit);

    // Estadísticas por status
    @Query("SELECT c.status, COUNT(c) FROM Case c GROUP BY c.status")
    List<Object[]> countByStatus();

    @Query("SELECT c.priority, COUNT(c) FROM Case c GROUP BY c.priority")
    List<Object[]> countByPriority();

    @Query("SELECT c.caseType, COUNT(c) FROM Case c GROUP BY c.caseType")
    List<Object[]> countByType();

    // Casos donde el usuario es colaborador
    @Query("SELECT c FROM Case c JOIN c.collaborators col WHERE col.userId = :userId")
    Page<Case> findCasesWhereUserIsCollaborator(@Param("userId") Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Case c SET c.status = :status, c.closedAt = CURRENT_TIMESTAMP WHERE c.id = :id")
    void closeCase(@Param("id") Long id, @Param("status") Case.CaseStatus status);

    @Modifying
    @Query("UPDATE Case c SET c.currentVersion = :version WHERE c.id = :id")
    void updateCurrentVersion(@Param("id") Long id, @Param("version") Integer version);

    @Query("SELECT COUNT(c) FROM Case c WHERE c.status = :status")
    long countByStatusEnum(@Param("status") Case.CaseStatus status);
}

package com.legal.cases.repository;
import com.legal.cases.model.CaseCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface CollaboratorRepository extends JpaRepository<CaseCollaborator, Long> {
    List<CaseCollaborator> findByLegalCaseId(Long caseId);
    Optional<CaseCollaborator> findByLegalCaseIdAndUserId(Long caseId, Long userId);
    boolean existsByLegalCaseIdAndUserId(Long caseId, Long userId);
    @Modifying
    @Query("DELETE FROM CaseCollaborator c WHERE c.legalCase.id = :caseId AND c.userId = :userId")
    void deleteByCaseIdAndUserId(@Param("caseId") Long caseId, @Param("userId") Long userId);
}

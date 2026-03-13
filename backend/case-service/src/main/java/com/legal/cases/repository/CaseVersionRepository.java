package com.legal.cases.repository;

import com.legal.cases.model.CaseVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CaseVersionRepository extends JpaRepository<CaseVersion, Long> {
    List<CaseVersion> findByLegalCaseIdOrderByVersionNumberDesc(Long caseId);
    Optional<CaseVersion> findByLegalCaseIdAndVersionNumber(Long caseId, Integer versionNumber);
    @Query("SELECT MAX(v.versionNumber) FROM CaseVersion v WHERE v.legalCase.id = :caseId")
    Optional<Integer> findMaxVersionNumber(@Param("caseId") Long caseId);
    long countByLegalCaseId(Long caseId);
}

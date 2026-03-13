package com.legal.cases.repository;

import com.legal.cases.model.VersionComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<VersionComment, Long> {
    List<VersionComment> findByVersionIdOrderByCreatedAtAsc(Long versionId);
    long countByVersionId(Long versionId);
}

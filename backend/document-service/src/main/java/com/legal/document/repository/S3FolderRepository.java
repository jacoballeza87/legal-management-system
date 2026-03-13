package com.legal.document.repository;

import com.legal.document.model.S3Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface S3FolderRepository extends JpaRepository<S3Folder, Long> {
    List<S3Folder> findByCaseId(Long caseId);
    Optional<S3Folder> findByCaseIdAndFolderPathContaining(Long caseId, String pathFragment);
}

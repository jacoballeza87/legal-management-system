package com.legal.document.dto;

import com.legal.document.model.CaseDocument.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentResponse {
    private Long id;
    private Long caseId;
    private String caseNumber;
    private String documentKey;
    private String originalFileName;
    private String mimeType;
    private Long fileSize;
    private String fileSizeFormatted;
    private String description;
    private Long uploadedByUserId;
    private String uploadedByName;
    private DocumentStatus status;
    private DocumentCategory category;
    private Integer version;
    private Long previousVersionId;
    private String downloadUrl;
    private String googleDriveUrl;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
}

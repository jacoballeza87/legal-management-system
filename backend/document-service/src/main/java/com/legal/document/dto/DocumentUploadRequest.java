package com.legal.document.dto;

import com.legal.document.model.CaseDocument.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DocumentUploadRequest {

    @NotNull
    private Long caseId;

    @NotBlank
    private String caseNumber;

    @NotNull
    private Long uploadedByUserId;

    private String uploadedByName;

    @Size(max = 500)
    private String description;

    @NotNull
    private DocumentCategory category;

    private Long previousVersionId;
}

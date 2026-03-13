package com.legal.cases.dto;

import com.legal.cases.model.CaseVersion;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VersionDTO {
    private Long id;
    private Long caseId;
    private Integer versionNumber;
    private String title;
    private String description;
    private String content;
    private String changeSummary;
    private Long createdBy;
    private String createdByName;
    private CaseVersion.VersionStatus status;
    private List<CommentDTO> comments;
    private LocalDateTime createdAt;
}

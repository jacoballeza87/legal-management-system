package com.legal.cases.dto;

import com.legal.cases.model.Case;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CaseDTO {
    private Long id;
    private String caseNumber;
    private String title;
    private String description;
    private Case.CaseStatus status;
    private Case.CasePriority priority;
    private Case.CaseType caseType;
    private Long ownerId;
    private String clientName;
    private String clientEmail;
    private String clientPhone;
    private String courtName;
    private String courtCaseNumber;
    private Long categoryId;
    private LocalDate dueDate;
    private LocalDateTime closedAt;
    private Double estimatedHours;
    private Double billedHours;
    private String qrCodeUrl;
    private Integer currentVersion;
    private String tags;
    private List<CollaboratorDTO> collaborators;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

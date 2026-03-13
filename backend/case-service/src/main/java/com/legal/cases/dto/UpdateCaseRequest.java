package com.legal.cases.dto;

import com.legal.cases.model.Case;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateCaseRequest {
    @Size(min = 5, max = 200)
    private String title;
    private String description;
    private Case.CaseStatus status;
    private Case.CasePriority priority;
    private Case.CaseType caseType;
    private String clientName;
    @Email private String clientEmail;
    private String clientPhone;
    private String courtName;
    private String courtCaseNumber;
    private Long categoryId;
    private LocalDate dueDate;
    private Double estimatedHours;
    private Double billedHours;
    private String tags;
}

package com.legal.cases.dto;

import com.legal.cases.model.Case;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CaseFilterRequest {
    private String search;
    private Case.CaseStatus status;
    private Case.CasePriority priority;
    private Case.CaseType caseType;
    private Long ownerId;
    private Long categoryId;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateTo;
    @Builder.Default
    private int page = 0;
    @Builder.Default
    private int size = 20;
    @Builder.Default
    private String sortBy = "createdAt";
    @Builder.Default
    private String direction = "desc";
}

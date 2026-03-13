package com.legal.cases.dto;

import com.legal.cases.model.Case;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateCaseRequest {
    @NotBlank(message = "El título es obligatorio")
    @Size(min = 5, max = 200)
    private String title;

    private String description;

    @NotNull(message = "El tipo de caso es obligatorio")
    private Case.CaseType caseType;

    @Builder.Default
    private Case.CasePriority priority = Case.CasePriority.MEDIUM;

    @NotBlank(message = "El nombre del cliente es obligatorio")
    private String clientName;

    @Email private String clientEmail;
    private String clientPhone;
    private String courtName;
    private String courtCaseNumber;
    private Long categoryId;
    private LocalDate dueDate;
    private Double estimatedHours;
    private String tags;
}

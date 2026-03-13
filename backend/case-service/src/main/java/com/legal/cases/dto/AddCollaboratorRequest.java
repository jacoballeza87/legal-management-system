package com.legal.cases.dto;

import com.legal.cases.model.CaseCollaborator;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddCollaboratorRequest {
    @NotNull(message = "El userId es obligatorio")
    private Long userId;
    private String userName;
    private String userEmail;
    @Builder.Default
    private CaseCollaborator.CollaboratorRole role = CaseCollaborator.CollaboratorRole.VIEWER;
}

package com.legal.cases.dto;

import com.legal.cases.model.CaseCollaborator;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CollaboratorDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private CaseCollaborator.CollaboratorRole role;
    private Long addedBy;
    private LocalDateTime addedAt;
}

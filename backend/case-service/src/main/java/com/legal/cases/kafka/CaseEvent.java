package com.legal.cases.kafka;

import com.legal.cases.model.Case;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CaseEvent {
    private String eventType;       // CREATED, UPDATED, DELETED, STATUS_CHANGED
    private Long caseId;
    private String caseNumber;
    private String title;
    private Case.CaseStatus status;
    private Case.CaseStatus previousStatus;
    private Long ownerId;
    private String clientName;
    private Long triggeredBy;       // userId que generó el evento
    private LocalDateTime occurredAt;
}

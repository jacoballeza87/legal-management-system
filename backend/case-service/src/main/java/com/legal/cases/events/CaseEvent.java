package com.legal.cases.events;

import com.legal.cases.model.Case;
import lombok.*;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaseEvent {

    private String eventType;
    private Long caseId;
    private String caseNumber;
    private String title;
    private Case.CaseStatus oldStatus;
    private Case.CaseStatus newStatus;
    private Long userId;
    private String userName;
    private Long collaboratorId;
    private LocalDateTime timestamp;

    // 👇 AGREGA ESTE CONSTRUCTOR SIMPLE
    public CaseEvent(Long caseId, String eventType, Long userId) {
        this.caseId = caseId;
        this.eventType = eventType;
        this.userId = userId;
        this.timestamp = LocalDateTime.now();
    }
}
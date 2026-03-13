package com.legal.cases.kafka;

import com.legal.cases.model.Case;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class CaseEventProducer {

    private final KafkaTemplate<String, CaseEvent> kafkaTemplate;

    @Value("${kafka.topics.case-created:case.created}")
    private String caseCreatedTopic;

    @Value("${kafka.topics.case-updated:case.updated}")
    private String caseUpdatedTopic;

    @Value("${kafka.topics.case-deleted:case.deleted}")
    private String caseDeletedTopic;

    @Value("${kafka.topics.case-status-changed:case.status.changed}")
    private String caseStatusChangedTopic;

    @Value("${kafka.topics.collaborator-added:case.collaborator.added}")
    private String collaboratorAddedTopic;

    public void publishCaseCreated(Case legalCase, Long triggeredBy) {
        CaseEvent event = buildEvent("CREATED", legalCase, null, triggeredBy);
        send(caseCreatedTopic, event);
    }

    public void publishCaseUpdated(Case legalCase, Long triggeredBy) {
        CaseEvent event = buildEvent("UPDATED", legalCase, null, triggeredBy);
        send(caseUpdatedTopic, event);
    }

    public void publishCaseDeleted(Long caseId, String caseNumber, Long triggeredBy) {
        CaseEvent event = CaseEvent.builder()
                .eventType("DELETED").caseId(caseId).caseNumber(caseNumber)
                .triggeredBy(triggeredBy).occurredAt(LocalDateTime.now()).build();
        send(caseDeletedTopic, event);
    }

    public void publishStatusChanged(Case legalCase, Case.CaseStatus previousStatus, Long triggeredBy) {
        CaseEvent event = buildEvent("STATUS_CHANGED", legalCase, previousStatus, triggeredBy);
        send(caseStatusChangedTopic, event);
    }

    public void publishCollaboratorAdded(Case legalCase, Long collaboratorUserId, Long triggeredBy) {
        CaseEvent event = CaseEvent.builder()
                .eventType("COLLABORATOR_ADDED").caseId(legalCase.getId())
                .caseNumber(legalCase.getCaseNumber()).title(legalCase.getTitle())
                .ownerId(legalCase.getOwnerId()).triggeredBy(triggeredBy)
                .occurredAt(LocalDateTime.now()).build();
        send(collaboratorAddedTopic, event);
    }

    private void send(String topic, CaseEvent event) {
        CompletableFuture<SendResult<String, CaseEvent>> future =
                kafkaTemplate.send(topic, event.getCaseNumber(), event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Error publicando evento {} para caso {}: {}",
                        event.getEventType(), event.getCaseNumber(), ex.getMessage());
            } else {
                log.debug("Evento {} publicado en topic {}", event.getEventType(), topic);
            }
        });
    }

    private CaseEvent buildEvent(String type, Case c, Case.CaseStatus prevStatus, Long triggeredBy) {
        return CaseEvent.builder()
                .eventType(type).caseId(c.getId()).caseNumber(c.getCaseNumber())
                .title(c.getTitle()).status(c.getStatus()).previousStatus(prevStatus)
                .ownerId(c.getOwnerId()).clientName(c.getClientName())
                .triggeredBy(triggeredBy).occurredAt(LocalDateTime.now()).build();
    }
}

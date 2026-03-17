package com.legal.cases.events;

import com.legal.cases.model.Case;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CaseEventProducer {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "case.exchange";

    public void publishCaseCreated(Case legalCase, Long userId) {
        CaseEvent event = new CaseEvent(
                legalCase.getId(),
                "CASE_CREATED",
                userId
        );

        rabbitTemplate.convertAndSend(EXCHANGE, "case.created", event);
    }

    public void publishCaseUpdated(Case legalCase, Long userId) {
        CaseEvent event = new CaseEvent(
                legalCase.getId(),
                "CASE_UPDATED",
                userId
        );

        rabbitTemplate.convertAndSend(EXCHANGE, "case.updated", event);
    }

    public void publishStatusChanged(Case legalCase, Case.CaseStatus previousStatus, Long userId) {
        CaseEvent event = new CaseEvent(
                legalCase.getId(),
                "CASE_STATUS_CHANGED",
                userId
        );

        rabbitTemplate.convertAndSend(EXCHANGE, "case.status.changed", event);
    }

    public void publishCollaboratorAdded(Case legalCase, Long collaboratorId, Long addedBy) {
        CaseEvent event = new CaseEvent(
                legalCase.getId(),
                "CASE_COLLABORATOR_ADDED",
                addedBy
        );

        rabbitTemplate.convertAndSend(EXCHANGE, "case.collaborator.added", event);
    }

    public void publishCaseDeleted(Long caseId, String caseNumber, Long userId) {
        CaseEvent event = new CaseEvent(
                caseId,
                "CASE_DELETED",
                userId
        );

        rabbitTemplate.convertAndSend(EXCHANGE, "case.deleted", event);
    }
}
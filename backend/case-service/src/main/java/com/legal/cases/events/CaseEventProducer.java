package com.legal.cases.events;

import com.legal.cases.config.RabbitMQConfig;
import com.legal.cases.model.Case;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CaseEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publishCaseCreated(Case legalCase, Long userId) {
        CaseEvent event = new CaseEvent(
                legalCase.getId(),
                "CASE_CREATED",
                userId
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.LEGAL_EXCHANGE, RabbitMQConfig.ROUTING_KEY_CREATED, event);
    }

    public void publishCaseUpdated(Case legalCase, Long userId) {
        CaseEvent event = new CaseEvent(
                legalCase.getId(),
                "CASE_UPDATED",
                userId
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.LEGAL_EXCHANGE, RabbitMQConfig.ROUTING_KEY_UPDATED, event);
    }

    public void publishStatusChanged(Case legalCase, Case.CaseStatus previousStatus, Long userId) {
        CaseEvent event = new CaseEvent(
                legalCase.getId(),
                "CASE_STATUS_CHANGED",
                userId
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.LEGAL_EXCHANGE, RabbitMQConfig.ROUTING_KEY_STATUS_CHANGED, event);
    }

    public void publishCollaboratorAdded(Case legalCase, Long collaboratorId, Long addedBy) {
        CaseEvent event = new CaseEvent(
                legalCase.getId(),
                "CASE_COLLABORATOR_ADDED",
                addedBy
        );

        // Si se requiere un routing key específico para colaboradores, asegúrate de declararlo en RabbitMQConfig. 
        // Usamos la clave estándar o su respectivo binding.
        rabbitTemplate.convertAndSend(RabbitMQConfig.LEGAL_EXCHANGE, "case.collaborator.added", event);
    }

    public void publishCaseDeleted(Long caseId, String caseNumber, Long userId) {
        CaseEvent event = new CaseEvent(
                caseId,
                "CASE_DELETED",
                userId
        );

        rabbitTemplate.convertAndSend(RabbitMQConfig.LEGAL_EXCHANGE, RabbitMQConfig.ROUTING_KEY_DELETED, event);
    }
}

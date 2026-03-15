package com.legal.cases.events;

import com.legal.cases.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CaseEventProducer {

    private final RabbitTemplate rabbitTemplate;

    private static final String EXCHANGE = "legal.exchange";

    public void publishCaseCreated(Case legalCase, Long triggeredBy) {

        CaseEvent event = buildEvent("CASE_CREATED", legalCase, null, triggeredBy);

        rabbitTemplate.convertAndSend(
                EXCHANGE,
                "case.created",
                event
        );

        log.debug("Evento CASE_CREATED enviado");
    }

    public void publishCaseUpdated(Case legalCase, Long triggeredBy) {

        CaseEvent event = buildEvent("CASE_UPDATED", legalCase, null, triggeredBy);

        rabbitTemplate.convertAndSend(
                EXCHANGE,
                "case.updated",
                event
        );
    }

    private CaseEvent buildEvent(String type, Case c,
                                 Case.CaseStatus prevStatus,
                                 Long triggeredBy) {

        return CaseEvent.builder()
                .eventType(type)
                .caseId(c.getId())
                .caseNumber(c.getCaseNumber())
                .title(c.getTitle())
                .status(c.getStatus())
                .previousStatus(prevStatus)
                .ownerId(c.getOwnerId())
                .clientName(c.getClientName())
                .triggeredBy(triggeredBy)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}
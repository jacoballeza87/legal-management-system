package com.legal.cases.events;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CaseEventListener {

    @RabbitListener(queues = "user.deleted.queue")
    public void onUserDeleted(Object event) {

        log.info("Evento user.deleted recibido: {}", event);

    }

    @RabbitListener(queues = "document.uploaded.queue")
    public void onDocumentUploaded(Object event) {

        log.debug("Documento subido: {}", event);

    }
}
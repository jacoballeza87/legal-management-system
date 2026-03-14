package com.legal.cases.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CaseEventListener {

    // Ejemplo: escuchar si un usuario fue eliminado para actualizar casos
    public void onUserDeleted(Object event) {
        log.info("Evento user.deleted recibido: {}", event);
        // TODO: reasignar o notificar casos del usuario eliminado
    }


    public void onDocumentUploaded(Object event) {
        log.debug("Documento subido: {}", event);
        // TODO: vincular documento a versión de caso si corresponde
    }
}

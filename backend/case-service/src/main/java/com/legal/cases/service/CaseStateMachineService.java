package com.legal.cases.service;

import com.legal.cases.enums.CaseStatus;
import org.springframework.stereotype.Service;

import java.util.EnumSet;

@Service
public class CaseStateMachineService {

    private static final EnumSet<CaseStatus> REOPENABLE_STATES = EnumSet.of(
            CaseStatus.CREATED,
            CaseStatus.ASSIGNED,
            CaseStatus.UPDATED,
            CaseStatus.IN_APPROVAL,
            CaseStatus.CLOSED,
            CaseStatus.COMPLETED,
            CaseStatus.TERMINATED_WITHOUT_SUCCESS,
            CaseStatus.TERMINATED_WITH_SUCCESS,
            CaseStatus.REOPENED
    );

    public boolean canTransition(CaseStatus currentState, CaseStatus targetStatus) {
        if (currentState == null) {
            return targetStatus == CaseStatus.CREATED;
        }

        // RN-F-004: Solo los casos con estado CERTIFIED no podrán ser reabiertos
        if (currentState == CaseStatus.CERTIFIED) {
            return targetStatus == CaseStatus.CERTIFIED; // Inmutable ante reaperturas
        }

        if (targetStatus == CaseStatus.REOPENED) {
            return REOPENABLE_STATES.contains(currentState);
        }

        return true;
    }

    public void validateTransition(CaseStatus currentState, CaseStatus targetStatus) {
        if (!canTransition(currentState, targetStatus)) {
            throw new IllegalStateException(
                String.format("Transición de estado inválida: No se puede cambiar de %s a %s. Los casos certificados son inmutables.", 
                currentState, targetStatus)
            );
        }
    }
}

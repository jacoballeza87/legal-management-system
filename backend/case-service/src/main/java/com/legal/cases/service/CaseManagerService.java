package com.legal.cases.service;

import com.legal.cases.models.LegalCase;
import com.legal.cases.models.CaseUpdate;
import com.legal.cases.enums.CaseStatus;
import org.springframework.stereotype.Service;
import org.springframework.kafka.core.KafkaTemplate;

@Service
public class CaseManagerService {
    
    private final CaseRepository caseRepo;
    private final UpdateRepository updateRepo;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // RN-F-004: Certificación de Casos
    public LegalCase updateStatus(Long caseId, CaseStatus newStatus) {
        LegalCase legalCase = caseRepo.findById(caseId).orElseThrow();
        
        if (legalCase.getStatus() == CaseStatus.CERTIFICADO && newStatus != CaseStatus.CERTIFICADO) {
            throw new IllegalStateException("RN-F-004: Los casos certificados no pueden ser reabiertos ni modificados.");
        }
        
        legalCase.setStatus(newStatus);
        
        // RN-F-018: Si el estado es CREADO, generar evento para PDF
        if(newStatus == CaseStatus.CREADO) {
            kafkaTemplate.send("case-events", "CASE_CREATED", legalCase);
        }
        
        return caseRepo.save(legalCase);
    }

    // RN-O-001 & RN-O-003: Versionamiento Inmutable
    public CaseUpdate addVersionUpdate(Long caseId, Long userId, String comments) {
        int nextVersion = updateRepo.countByCaseId(caseId) + 1;
        CaseUpdate update = new CaseUpdate(caseId, userId, comments, nextVersion);
        CaseUpdate saved = updateRepo.save(update);
        
        // RN-F-019 & RN-F-021: Alerta y PDF por nueva versión
        kafkaTemplate.send("case-updates", "VERSION_ADDED", saved);
        return saved;
    }
}

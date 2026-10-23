package com.legal.docs.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
public class StorageService {

    // RN-F-016: Límite de carga
    public void uploadEvidences(Long caseId, List<MultipartFile> files, String userRole) {
        if (files.size() > 10) {
            throw new IllegalArgumentException("RN-F-016: Máximo 10 archivos por operación.");
        }

        // RN-F-015: Bucket S3 Privado para SYSADMIN
        if ("SYSADMIN".equals(userRole)) {
            uploadToAwsS3PrivateBucket(files);
            return;
        }

        // RN-F-012: Repositorio en Google Drive estructurado
        String folderName = getCaseDossierNumber(caseId);
        uploadToGoogleDrive(folderName, files);
    }
    
    // RN-F-013 & RN-O-007: Relación de carpetas (EXP1--EXP2)
    public void createSharedParentChildFolder(String dossierPadre, String dossierHijo) {
        String sharedFolderName = dossierPadre + "--" + dossierHijo;
        createGoogleDriveFolder(sharedFolderName);
    }
}

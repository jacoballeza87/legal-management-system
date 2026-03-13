package com.legal.document.service;

import com.google.api.client.http.InputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleDriveService {

    private final Drive driveService;   // Puede ser null si Drive está deshabilitado

    @Value("${google.drive.root-folder-id:}")
    private String rootFolderId;

    @Value("${google.drive.enabled:false}")
    private boolean driveEnabled;

    public String createCaseFolder(String caseNumber, String lawyerEmail) throws Exception {
        if (!isAvailable()) return null;

        File meta = new File();
        meta.setName(caseNumber);
        meta.setMimeType("application/vnd.google-apps.folder");
        if (rootFolderId != null && !rootFolderId.isBlank()) {
            meta.setParents(Collections.singletonList(rootFolderId));
        }

        File folder = driveService.files().create(meta)
            .setFields("id, webViewLink")
            .execute();

        if (lawyerEmail != null && !lawyerEmail.isBlank()) {
            shareWithUser(folder.getId(), lawyerEmail, "writer");
        }

        log.info("Carpeta Drive creada para caso {}: folderId={}", caseNumber, folder.getId());
        return folder.getId();
    }

    public String uploadFileToDrive(MultipartFile file, String folderId) throws Exception {
        if (!isAvailable()) return null;

        File meta = new File();
        meta.setName(file.getOriginalFilename());
        meta.setParents(Collections.singletonList(folderId));

        InputStreamContent content = new InputStreamContent(
            file.getContentType(), file.getInputStream());

        File uploaded = driveService.files().create(meta, content)
            .setFields("id, webViewLink")
            .execute();

        log.info("Archivo subido a Drive: {} | id={}", file.getOriginalFilename(), uploaded.getId());
        return uploaded.getId();
    }

    public String getFileViewLink(String fileId) throws Exception {
        if (!isAvailable()) return null;
        return driveService.files().get(fileId).setFields("webViewLink").execute().getWebViewLink();
    }

    public void shareWithUser(String fileId, String email, String role) throws Exception {
        if (!isAvailable()) return;
        Permission p = new Permission().setType("user").setRole(role).setEmailAddress(email);
        driveService.permissions().create(fileId, p).execute();
    }

    public void deleteFile(String fileId) throws Exception {
        if (!isAvailable()) return;
        driveService.files().delete(fileId).execute();
        log.info("Archivo Drive eliminado: {}", fileId);
    }

    private boolean isAvailable() {
        if (!driveEnabled || driveService == null) {
            log.debug("Google Drive no disponible.");
            return false;
        }
        return true;
    }
}

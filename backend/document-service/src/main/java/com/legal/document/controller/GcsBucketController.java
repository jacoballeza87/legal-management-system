package com.legal.document.controller;

import com.legal.document.service.GcsService;
import com.google.cloud.storage.Blob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@ConditionalOnProperty(name = "gcp.storage.enabled", havingValue = "true", matchIfMissing = false)
@RequestMapping("/api/admin/gcs")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class GcsBucketController {

    private final GcsService gcsService;

    /** Health-check de la conexión a Cloud Storage */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> gcsHealth() {
        try {
            // A diferencia de S3, GCS regresa null (no lanza excepción) si el objeto
            // no existe -- una respuesta null aquí ya confirma que la conexión sí opera.
            Blob probe = gcsService.getObjectMetadata("health-check-probe");
            return ResponseEntity.ok(Map.of(
                "status",  "OK",
                "bucket",  gcsService.getBucketName(),
                "message", probe == null ? "Conexión GCS activa (objeto de prueba no existe, esperado)"
                                          : "Conexión GCS activa"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                "status",  "ERROR",
                "message", "Cloud Storage no disponible: " + e.getMessage()
            ));
        }
    }
}

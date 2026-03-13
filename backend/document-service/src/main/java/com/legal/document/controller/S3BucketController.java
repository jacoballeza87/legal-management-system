package com.legal.document.controller;

import com.legal.document.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/s3")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ROLE_SUPER_ADMIN')")
public class S3BucketController {

    private final S3Service s3Service;

    /** Health-check de la conexión S3 */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> s3Health() {
        try {
            // Intentar obtener metadata del bucket (valida credenciales)
            s3Service.getObjectMetadata("health-check-probe");
        } catch (Exception e) {
            // Si el objeto no existe es igualmente una respuesta válida de S3
            if (e.getMessage() != null && e.getMessage().contains("NoSuchKey")) {
                return ResponseEntity.ok(Map.of(
                    "status",  "OK",
                    "bucket",  s3Service.getBucketName(),
                    "message", "Conexión S3 activa"
                ));
            }
            return ResponseEntity.status(503).body(Map.of(
                "status",  "ERROR",
                "message", "S3 no disponible: " + e.getMessage()
            ));
        }
        return ResponseEntity.ok(Map.of("status", "OK", "bucket", s3Service.getBucketName()));
    }
}

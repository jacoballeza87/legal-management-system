package com.legal.document.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class FileValidationService {

    private final Tika tika = new Tika();

    @Value("${document.max-size-bytes:52428800}")
    private long maxSizeBytes;

    private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>(Arrays.asList(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/msword",
        "application/vnd.ms-excel",
        "image/png",
        "image/jpeg",
        "text/plain",
        "application/zip",
        "application/x-rar-compressed",
        "application/octet-stream"  // fallback para algunos zips
    ));

    public void validate(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException(
                "El archivo excede el tamaño máximo de " + (maxSizeBytes / 1024 / 1024) + " MB");
        }

        // Detectar tipo MIME real con Apache Tika (no confiar en el header del cliente)
        String detectedMime;
        try {
            detectedMime = tika.detect(file.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer el archivo: " + e.getMessage());
        }

        if (!ALLOWED_MIME_TYPES.contains(detectedMime)) {
            throw new IllegalArgumentException(
                "Tipo de archivo no permitido: " + detectedMime);
        }

        // Prevenir path traversal en el nombre
        String filename = file.getOriginalFilename();
        if (filename == null || filename.contains("..") ||
            filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Nombre de archivo inválido");
        }

        log.debug("Archivo validado OK: {} | MIME: {} | {} bytes",
            filename, detectedMime, file.getSize());
    }

    public String detectMimeType(MultipartFile file) throws IOException {
        return tika.detect(file.getInputStream());
    }

    public String calculateChecksum(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            log.warn("No se pudo calcular checksum: {}", e.getMessage());
            return "unknown";
        }
    }
}

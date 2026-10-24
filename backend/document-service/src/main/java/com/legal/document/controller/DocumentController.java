package com.legal.documents.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/documents") // <-- Alineado con el rewrite del Gateway (/api/documents/**)
public class DocumentController {

    @GetMapping("/health")
    public String healthCheck() {
        return "Document Service is up and running";
    }
    
    // Endpoints de gestión de documentos y GCS...
}

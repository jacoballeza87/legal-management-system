package com.legal.cases.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateVersionRequest {
    @NotBlank(message = "El título de la versión es obligatorio")
    @Size(min = 3, max = 200)
    private String title;

    private String description;

    @NotBlank(message = "El contenido es obligatorio")
    private String content;

    @Size(max = 500)
    private String changeSummary;
}

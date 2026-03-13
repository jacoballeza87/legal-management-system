package com.legal.cases.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateCommentRequest {
    @NotBlank(message = "El contenido del comentario es obligatorio")
    @Size(min = 1, max = 2000)
    private String content;
}

package com.legal.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateCategoryRequest {
    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(min = 2, max = 100)
    private String name;

    private String description;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color hex inválido (ej: #FF5733)")
    private String colorHex;

    private String icon;
}

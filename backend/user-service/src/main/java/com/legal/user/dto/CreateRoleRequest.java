package com.legal.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateRoleRequest {
    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(min = 2, max = 50)
    private String name;

    private String description;
    private Set<Long> permissionIds;
}

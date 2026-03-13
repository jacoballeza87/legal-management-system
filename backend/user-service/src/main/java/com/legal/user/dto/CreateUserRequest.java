package com.legal.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateUserRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100)
    private String name;

    @NotBlank(message = "El username es obligatorio")
    @Size(min = 3, max = 50)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username inválido")
    private String username;

    @NotBlank(message = "El email es obligatorio")
    @Email
    private String email;

    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Teléfono inválido")
    private String phone;

    private String jobTitle;
    private String department;
    private String barNumber;
    private String bio;
    private Set<Long> roleIds;
    private Set<Long> categoryIds;
}

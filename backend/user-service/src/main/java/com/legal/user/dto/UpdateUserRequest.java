package com.legal.user.dto;

import com.legal.user.model.User;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateUserRequest {
    @Size(min = 2, max = 100)
    private String name;

    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Teléfono inválido")
    private String phone;

    private String avatarUrl;
    private String bio;
    private String jobTitle;
    private String department;
    private String barNumber;
    private User.UserStatus status;
    private Set<Long> roleIds;
    private Set<Long> categoryIds;
}

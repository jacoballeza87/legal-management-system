package com.legal.user.dto;

import com.legal.user.model.User;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDTO {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String phone;
    private String avatarUrl;
    private String bio;
    private String jobTitle;
    private String department;
    private String barNumber;
    private User.UserStatus status;
    private Boolean isEmailVerified;
    private LocalDateTime lastLogin;
    private Set<RoleDTO> roles;
    private Set<CategoryDTO> categories;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

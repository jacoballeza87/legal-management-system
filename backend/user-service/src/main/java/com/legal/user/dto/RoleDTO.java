package com.legal.user.dto;

import lombok.*;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoleDTO {
    private Long id;
    private String name;
    private String description;
    private Boolean isSystemRole;
    private Set<PermissionDTO> permissions;
}

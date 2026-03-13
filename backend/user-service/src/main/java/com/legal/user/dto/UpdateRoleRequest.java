package com.legal.user.dto;

import lombok.*;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateRoleRequest {
    private String description;
    private Set<Long> permissionIds;
}

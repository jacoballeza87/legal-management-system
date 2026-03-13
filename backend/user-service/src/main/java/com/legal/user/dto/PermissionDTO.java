package com.legal.user.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PermissionDTO {
    private Long id;
    private String name;
    private String description;
    private String module;
    private String action;
}

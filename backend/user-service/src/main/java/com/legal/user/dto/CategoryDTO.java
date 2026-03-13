package com.legal.user.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryDTO {
    private Long id;
    private String name;
    private String description;
    private String colorHex;
    private String icon;
    private Boolean isActive;
}

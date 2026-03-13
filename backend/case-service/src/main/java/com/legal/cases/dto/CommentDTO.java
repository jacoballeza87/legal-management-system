package com.legal.cases.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommentDTO {
    private Long id;
    private Long versionId;
    private String content;
    private Long authorId;
    private String authorName;
    private Boolean isEdited;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

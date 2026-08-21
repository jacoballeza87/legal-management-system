package com.legal.cases.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityDTO {
    private Long caseId;
    private String caseNumber;
    private String title;
    private String status;
    private Long ownerId;
    private LocalDateTime updatedAt;
}

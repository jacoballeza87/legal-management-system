package com.legal.cases.dto;

import lombok.*;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CaseStatsDTO {
    private long total;
    private Map<String, Long> byStatus;
    private Map<String, Long> byPriority;
    private Map<String, Long> byType;
    private long overdue;
    private long dueSoon;    // Vencen en los próximos 7 días
}

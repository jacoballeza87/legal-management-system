package com.legal.cases.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "case_relations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"source_case_id", "target_case_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CaseRelation {

    @Id
    @Column(name= "relation_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_case_id", nullable = false)
    private Case sourceCase;

    @Column(name = "target_case_id", nullable = false)
    private Long targetCaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false, length = 30)
    private RelationType relationType;

    @Column(length = 255)
    private String notes;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum RelationType {
        RELATED, PARENT, CHILD, DUPLICATE, APPEAL, CONSOLIDATED
    }
}

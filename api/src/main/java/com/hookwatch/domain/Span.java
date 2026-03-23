package com.hookwatch.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "spans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Span {

    public enum Type { LLM_CALL, TOOL_CALL, RETRIEVAL, CUSTOM }
    public enum Status { RUNNING, COMPLETED, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trace_id", nullable = false)
    private UUID traceId;

    @Column(name = "parent_span_id")
    private UUID parentSpanId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    private Instant completedAt;

    private Integer inputTokens;
    private Integer outputTokens;

    @Column(precision = 12, scale = 6)
    private BigDecimal cost;

    private String model;

    @Column(columnDefinition = "TEXT")
    private String input;

    @Column(columnDefinition = "TEXT")
    private String output;

    @Column(columnDefinition = "TEXT")
    private String error;

    private Integer sortOrder;

    @PrePersist
    protected void onCreate() {
        startedAt = Instant.now();
    }
}

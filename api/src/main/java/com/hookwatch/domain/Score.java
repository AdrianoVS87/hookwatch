package com.hookwatch.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Evaluation score attached to a Trace.
 * Supports numeric (0-1 float), categorical (string label), and boolean values.
 * Inspired by Langfuse scoring model for LLM quality evaluation.
 */
@Entity
@Table(name = "scores")
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trace_id", nullable = false)
    private Trace trace;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false)
    private DataType dataType;

    @Column(name = "numeric_value")
    private Double numericValue;

    @Column(name = "string_value", length = 255)
    private String stringValue;

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    public enum DataType { NUMERIC, CATEGORICAL, BOOLEAN }
    public enum Source { API, MANUAL, LLM_JUDGE }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Trace getTrace() { return trace; }
    public void setTrace(Trace trace) { this.trace = trace; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public DataType getDataType() { return dataType; }
    public void setDataType(DataType dataType) { this.dataType = dataType; }

    public Double getNumericValue() { return numericValue; }
    public void setNumericValue(Double numericValue) { this.numericValue = numericValue; }

    public String getStringValue() { return stringValue; }
    public void setStringValue(String stringValue) { this.stringValue = stringValue; }

    public Boolean getBooleanValue() { return booleanValue; }
    public void setBooleanValue(Boolean booleanValue) { this.booleanValue = booleanValue; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}

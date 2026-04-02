package com.hookwatch.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Groups recurring failure patterns by hashing error message + span type + model.
 * Upserted on trace ingestion when a span has error status.
 */
@Entity
@Table(name = "failure_fingerprints", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "agent_id", "hash"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailureFingerprint {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    /** SHA-256 of (errorMessage + spanType + model), lowercase trimmed. */
    @Column(nullable = false, length = 64)
    private String hash;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "span_type", length = 32)
    private String spanType;

    @Column(length = 128)
    private String model;

    @Column(name = "first_seen_at", nullable = false, updatable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount;

    @PrePersist
    protected void onCreate() {
        if (firstSeenAt == null) firstSeenAt = Instant.now();
        if (lastSeenAt == null) lastSeenAt = Instant.now();
    }
}

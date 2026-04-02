package com.hookwatch.service;

import com.hookwatch.domain.FailureFingerprint;
import com.hookwatch.domain.Span;
import com.hookwatch.dto.FingerprintDto;
import com.hookwatch.repository.FailureFingerprintRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

/**
 * Computes SHA-256 fingerprints for failed spans and upserts occurrence data.
 * Provides grouped listing and daily trend timeseries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FingerprintService {

    private final FailureFingerprintRepository repository;
    private final EntityManager em;

    /**
     * Computes a SHA-256 fingerprint from a span's error context.
     *
     * @param span the failed span
     * @return hex-encoded SHA-256 hash
     */
    public String computeFingerprint(Span span) {
        String errorMsg = span.getError() != null ? span.getError().trim().toLowerCase(Locale.ROOT) : "";
        String spanType = span.getType() != null ? span.getType().name().toLowerCase(Locale.ROOT) : "";
        String model = span.getModel() != null ? span.getModel().trim().toLowerCase(Locale.ROOT) : "";
        String input = errorMsg + "|" + spanType + "|" + model;

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed by the JVM spec — this cannot happen
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * Groups failed spans by fingerprint hash.
     */
    public Map<String, List<Span>> groupByHash(List<Span> spans) {
        if (spans == null || spans.isEmpty()) return Map.of();
        Map<String, List<Span>> grouped = new LinkedHashMap<>();
        for (Span span : spans) {
            if (span.getStatus() == Span.Status.FAILED && span.getError() != null && !span.getError().isBlank()) {
                String hash = computeFingerprint(span);
                grouped.computeIfAbsent(hash, k -> new ArrayList<>()).add(span);
            }
        }
        return grouped;
    }

    /**
     * Upserts a fingerprint for a failed span during trace ingestion.
     * Thread-safe: uses findByHash + save with retry-on-conflict semantics.
     */
    @Transactional
    public void upsertFingerprint(UUID tenantId, UUID agentId, Span span) {
        if (span.getStatus() != Span.Status.FAILED || span.getError() == null || span.getError().isBlank()) {
            return;
        }

        String hash = computeFingerprint(span);
        Optional<FailureFingerprint> existing = repository.findByTenantIdAndAgentIdAndHash(tenantId, agentId, hash);

        if (existing.isPresent()) {
            FailureFingerprint fp = existing.get();
            fp.setOccurrenceCount(fp.getOccurrenceCount() + 1);
            fp.setLastSeenAt(Instant.now());
            repository.save(fp);
        } else {
            FailureFingerprint fp = FailureFingerprint.builder()
                    .tenantId(tenantId)
                    .agentId(agentId)
                    .hash(hash)
                    .errorMessage(truncate(span.getError(), 1000))
                    .spanType(span.getType() != null ? span.getType().name() : null)
                    .model(span.getModel())
                    .firstSeenAt(Instant.now())
                    .lastSeenAt(Instant.now())
                    .occurrenceCount(1)
                    .build();
            repository.save(fp);
        }
    }

    /**
     * Returns all fingerprints for an agent, sorted by occurrence count descending.
     */
    @Transactional(readOnly = true)
    public List<FingerprintDto.FingerprintSummary> getFingerprints(UUID agentId) {
        return repository.findByAgentIdOrderByOccurrenceCountDesc(agentId)
                .stream()
                .map(FingerprintDto.FingerprintSummary::fromEntity)
                .toList();
    }

    /**
     * Returns daily occurrence timeseries for a specific fingerprint within a date range.
     * Uses native SQL for date_trunc aggregation.
     */
    @Transactional(readOnly = true)
    public FingerprintDto.TrendResponse getTrend(UUID fingerprintId, LocalDate from, LocalDate to) {
        FailureFingerprint fp = repository.findById(fingerprintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fingerprint not found"));

        // Query traces that match this fingerprint's hash pattern within the date range
        String sql = """
                WITH daily AS (
                    SELECT date_trunc('day', t.started_at)::date AS day, COUNT(*) AS cnt
                    FROM traces t
                    JOIN spans s ON s.trace_id = t.id
                    WHERE t.agent_id = :agentId
                      AND s.status = 'FAILED'
                      AND s.error IS NOT NULL
                      AND t.started_at >= :from
                      AND t.started_at < :to
                      AND LOWER(TRIM(COALESCE(s.error, ''))) || '|' ||
                          LOWER(COALESCE(s.type::text, '')) || '|' ||
                          LOWER(TRIM(COALESCE(s.model, '')))
                          LIKE :hashInput
                    GROUP BY day
                    ORDER BY day ASC
                )
                SELECT day::text, cnt FROM daily
                """;

        // Reconstruct the hash input pattern for matching
        String errorMsg = fp.getErrorMessage() != null ? fp.getErrorMessage().trim().toLowerCase(Locale.ROOT) : "";
        String spanType = fp.getSpanType() != null ? fp.getSpanType().toLowerCase(Locale.ROOT) : "";
        String model = fp.getModel() != null ? fp.getModel().trim().toLowerCase(Locale.ROOT) : "";
        String hashInput = errorMsg + "|" + spanType + "|" + model;

        Query q = em.createNativeQuery(sql);
        q.setParameter("agentId", fp.getAgentId());
        q.setParameter("from", java.sql.Date.valueOf(from));
        q.setParameter("to", java.sql.Date.valueOf(to.plusDays(1)));
        q.setParameter("hashInput", hashInput);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<FingerprintDto.TrendPoint> trend = rows.stream()
                .map(r -> new FingerprintDto.TrendPoint(r[0].toString(), ((Number) r[1]).intValue()))
                .toList();

        return new FingerprintDto.TrendResponse(fingerprintId.toString(), trend);
    }

    private static String truncate(String value, int maxLen) {
        if (value == null) return null;
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

package com.hookwatch.service;

import com.hookwatch.dto.AnalyticsDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final EntityManager em;

    @Transactional(readOnly = true)
    public AnalyticsDto getAnalytics(UUID agentId, LocalDate from, LocalDate to, String granularity, String model) {
        List<AnalyticsDto.DailyUsage> dailyUsage = getDailyUsage(agentId, from, to, model);
        List<AnalyticsDto.ModelUsage> byModel = getModelUsage(agentId, from, to, model);
        List<AnalyticsDto.TopTrace> topTraces = getTopExpensiveTraces(agentId, from, to, model);
        AnalyticsDto.CostTrend costTrend = getCostTrend(agentId, from, to, model);

        List<AnalyticsDto.MemoryLineage> memoryLineage = getMemoryLineage(agentId, from, to, model);
        List<AnalyticsDto.FailureFingerprint> failureFingerprints = getFailureFingerprints(agentId, from, to, model);
        List<AnalyticsDto.FailureFingerprintTrend> failureFingerprintTrends = getFailureFingerprintTrends(agentId, from, to, model);
        AnalyticsDto.LearningVelocity learningVelocity = getLearningVelocity(agentId, from, to, model, failureFingerprints);
        List<AnalyticsDto.LearningVelocityByModel> learningVelocityByModel = getLearningVelocityByModel(agentId, from, to, model);
        AnalyticsDto.OTelCompliance otelCompliance = getOtelCompliance(agentId, from, to, model);
        AnalyticsDto.EvalLoopSummary evalLoopSummary = getEvalLoopSummary(agentId, from, to, model);

        return new AnalyticsDto(
                dailyUsage,
                byModel,
                topTraces,
                costTrend,
                memoryLineage,
                learningVelocity,
                learningVelocityByModel,
                failureFingerprints,
                failureFingerprintTrends,
                otelCompliance,
                evalLoopSummary
        );
    }

    private List<AnalyticsDto.DailyUsage> getDailyUsage(UUID agentId, LocalDate from, LocalDate to, String model) {
        String modelClause = model != null ? "AND t.metadata->>'model' = :model" : "";
        String sql = """
                SELECT
                    date_trunc('day', t.started_at)::date                         AS day,
                    COALESCE(SUM(t.total_tokens), 0)                               AS total_tokens,
                    COALESCE(SUM(t.total_cost), 0)                                 AS total_cost,
                    COUNT(*)                                                       AS trace_count,
                    COALESCE(AVG(EXTRACT(EPOCH FROM (t.completed_at - t.started_at)) * 1000), 0) AS avg_latency_ms,
                    COALESCE(
                        SUM(CASE WHEN t.status = 'FAILED' THEN 1 ELSE 0 END)::numeric / NULLIF(COUNT(*), 0),
                        0
                    )                                                              AS error_rate
                FROM traces t
                WHERE t.agent_id = :agentId
                  AND t.started_at >= :from
                  AND t.started_at < :to
                  """ + modelClause + """
                GROUP BY date_trunc('day', t.started_at)::date
                ORDER BY day ASC
                """;

        Query q = em.createNativeQuery(sql);
        bindCommon(q, agentId, from, to, model);

        List<Object[]> rows = q.getResultList();
        List<AnalyticsDto.DailyUsage> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new AnalyticsDto.DailyUsage(
                    row[0].toString(),
                    ((Number) row[1]).intValue(),
                    ((Number) row[2]).doubleValue(),
                    ((Number) row[3]).intValue(),
                    ((Number) row[4]).doubleValue(),
                    ((Number) row[5]).doubleValue()
            ));
        }
        return result;
    }

    private List<AnalyticsDto.ModelUsage> getModelUsage(UUID agentId, LocalDate from, LocalDate to, String model) {
        String modelClause = model != null ? "AND t.metadata->>'model' = :model" : "";
        String sql = """
                SELECT
                    COALESCE(s.model, 'unknown')      AS model,
                    COALESCE(SUM(s.input_tokens + s.output_tokens), 0) AS total_tokens,
                    COALESCE(SUM(s.cost), 0)          AS total_cost,
                    COUNT(DISTINCT s.trace_id)         AS trace_count
                FROM spans s
                JOIN traces t ON t.id = s.trace_id
                WHERE t.agent_id = :agentId
                  AND t.started_at >= :from
                  AND t.started_at < :to
                  AND s.model IS NOT NULL
                  """ + modelClause + """
                GROUP BY s.model
                ORDER BY total_cost DESC
                """;

        Query q = em.createNativeQuery(sql);
        bindCommon(q, agentId, from, to, model);

        List<Object[]> rows = q.getResultList();
        List<AnalyticsDto.ModelUsage> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new AnalyticsDto.ModelUsage(
                    (String) row[0],
                    ((Number) row[1]).intValue(),
                    ((Number) row[2]).doubleValue(),
                    ((Number) row[3]).intValue()
            ));
        }
        return result;
    }

    private List<AnalyticsDto.TopTrace> getTopExpensiveTraces(UUID agentId, LocalDate from, LocalDate to, String model) {
        String modelClause = model != null ? "AND t.metadata->>'model' = :model" : "";
        String sql = """
                SELECT
                    t.id::text             AS trace_id,
                    COALESCE(t.total_cost, 0)  AS total_cost,
                    COALESCE(t.total_tokens, 0) AS total_tokens,
                    t.started_at::text     AS started_at
                FROM traces t
                WHERE t.agent_id = :agentId
                  AND t.started_at >= :from
                  AND t.started_at < :to
                  """ + modelClause + """
                ORDER BY t.total_cost DESC NULLS LAST
                LIMIT 10
                """;

        Query q = em.createNativeQuery(sql);
        bindCommon(q, agentId, from, to, model);

        List<Object[]> rows = q.getResultList();
        List<AnalyticsDto.TopTrace> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new AnalyticsDto.TopTrace(
                    (String) row[0],
                    ((Number) row[1]).doubleValue(),
                    ((Number) row[2]).intValue(),
                    row[3].toString()
            ));
        }
        return result;
    }

    private List<AnalyticsDto.MemoryLineage> getMemoryLineage(UUID agentId, LocalDate from, LocalDate to, String model) {
        String modelClause = model != null ? "AND t.metadata->>'model' = :model" : "";
        String sql = """
                SELECT
                    t.id::text,
                    COUNT(*) FILTER (WHERE s.type = 'RETRIEVAL') AS retrieval_count,
                    t.status::text,
                    t.started_at::text
                FROM traces t
                LEFT JOIN spans s ON s.trace_id = t.id
                WHERE t.agent_id = :agentId
                  AND t.started_at >= :from
                  AND t.started_at < :to
                  """ + modelClause + """
                GROUP BY t.id, t.status, t.started_at
                HAVING COUNT(*) FILTER (WHERE s.type = 'RETRIEVAL') > 0
                ORDER BY retrieval_count DESC, t.started_at DESC
                LIMIT 10
                """;

        Query q = em.createNativeQuery(sql);
        bindCommon(q, agentId, from, to, model);

        List<Object[]> rows = q.getResultList();
        return rows.stream().map(r -> new AnalyticsDto.MemoryLineage(
                (String) r[0],
                ((Number) r[1]).intValue(),
                (String) r[2],
                (String) r[3]
        )).collect(Collectors.toList());
    }

    private List<AnalyticsDto.FailureFingerprint> getFailureFingerprints(UUID agentId, LocalDate from, LocalDate to, String model) {
        String modelClause = model != null ? "AND t.metadata->>'model' = :model" : "";
        String sql = """
                WITH failed_traces AS (
                    SELECT t.id, COALESCE(t.metadata->>'failureFingerprint', '') AS metadata_fp
                    FROM traces t
                    WHERE t.agent_id = :agentId
                      AND t.started_at >= :from
                      AND t.started_at < :to
                      AND t.status = 'FAILED'
                      """ + modelClause + """
                ),
                fp AS (
                    SELECT
                        f.id,
                        CASE
                            WHEN f.metadata_fp <> '' THEN f.metadata_fp
                            WHEN EXISTS (
                                SELECT 1 FROM spans s
                                WHERE s.trace_id = f.id
                                  AND s.status = 'FAILED'
                                  AND COALESCE(s.error, '') ILIKE '%timeout%'
                            ) THEN 'TOOL_TIMEOUT'
                            WHEN EXISTS (
                                SELECT 1 FROM spans s
                                WHERE s.trace_id = f.id
                                  AND s.status = 'FAILED'
                                  AND (COALESCE(s.error, '') ILIKE '%429%' OR COALESCE(s.error, '') ILIKE '%rate limit%')
                            ) THEN 'RATE_LIMIT'
                            WHEN EXISTS (
                                SELECT 1 FROM spans s
                                WHERE s.trace_id = f.id
                                  AND s.status = 'FAILED'
                                  AND COALESCE(s.error, '') ILIKE '%context%'
                            ) THEN 'CONTEXT_OVERFLOW'
                            WHEN EXISTS (
                                SELECT 1 FROM spans s
                                WHERE s.trace_id = f.id
                                  AND s.status = 'FAILED'
                                  AND COALESCE(s.error, '') ILIKE '%retriev%'
                            ) THEN 'RETRIEVAL_NULL'
                            ELSE 'FAILED_UNKNOWN'
                        END AS fingerprint
                    FROM failed_traces f
                )
                SELECT fingerprint, COUNT(*) AS c
                FROM fp
                GROUP BY fingerprint
                ORDER BY c DESC
                LIMIT 8
                """;

        Query q = em.createNativeQuery(sql);
        bindCommon(q, agentId, from, to, model);
        List<Object[]> rows = q.getResultList();
        int total = rows.stream().mapToInt(r -> ((Number) r[1]).intValue()).sum();
        if (total == 0) return List.of();

        List<AnalyticsDto.FailureFingerprint> result = new ArrayList<>();
        for (Object[] row : rows) {
            int count = ((Number) row[1]).intValue();
            result.add(new AnalyticsDto.FailureFingerprint((String) row[0], count, (double) count / total));
        }
        return result;
    }

    private List<AnalyticsDto.LearningVelocityByModel> getLearningVelocityByModel(UUID agentId, LocalDate from, LocalDate to, String model) {
        String modelClause = model != null ? "AND t.metadata->>'model' = :model" : "";
        String sql = """
                SELECT
                    COALESCE(s.model, 'unknown') AS model,
                    AVG(CASE WHEN t.status = 'COMPLETED' THEN 1.0 ELSE 0.0 END) AS success_rate,
                    AVG(EXTRACT(EPOCH FROM (t.completed_at - t.started_at)) * 1000) AS avg_latency_ms,
                    AVG(COALESCE(t.total_cost, 0)) AS avg_cost,
                    AVG(CASE WHEN EXISTS (
                        SELECT 1 FROM spans sx WHERE sx.trace_id = t.id AND sx.type = 'RETRIEVAL'
                    ) THEN 1.0 ELSE 0.0 END) AS memory_hit_rate
                FROM traces t
                JOIN spans s ON s.trace_id = t.id
                WHERE t.agent_id = :agentId
                  AND t.started_at >= :from
                  AND t.started_at < :to
                  AND s.model IS NOT NULL
                  """ + modelClause + """
                GROUP BY s.model
                ORDER BY avg_cost DESC
                LIMIT 10
                """;

        Query q = em.createNativeQuery(sql);
        bindCommon(q, agentId, from, to, model);
        List<Object[]> rows = q.getResultList();

        List<AnalyticsDto.LearningVelocityByModel> result = new ArrayList<>();
        for (Object[] r : rows) {
            result.add(new AnalyticsDto.LearningVelocityByModel(
                    (String) r[0],
                    r[1] == null ? 0.0 : ((Number) r[1]).doubleValue(),
                    r[2] == null ? 0.0 : ((Number) r[2]).doubleValue(),
                    r[3] == null ? 0.0 : ((Number) r[3]).doubleValue(),
                    r[4] == null ? 0.0 : ((Number) r[4]).doubleValue()
            ));
        }
        return result;
    }

    private List<AnalyticsDto.FailureFingerprintTrend> getFailureFingerprintTrends(UUID agentId, LocalDate from, LocalDate to, String model) {
        String modelClause = model != null ? "AND t.metadata->>'model' = :model" : "";
        String sql = """
                WITH failed AS (
                    SELECT
                        date_trunc('day', t.started_at)::date AS day,
                        CASE
                            WHEN COALESCE(t.metadata->>'failureFingerprint', '') <> '' THEN t.metadata->>'failureFingerprint'
                            WHEN EXISTS (SELECT 1 FROM spans s WHERE s.trace_id = t.id AND COALESCE(s.error, '') ILIKE '%%timeout%%') THEN 'TOOL_TIMEOUT'
                            WHEN EXISTS (SELECT 1 FROM spans s WHERE s.trace_id = t.id AND (COALESCE(s.error, '') ILIKE '%%429%%' OR COALESCE(s.error, '') ILIKE '%%rate limit%%')) THEN 'RATE_LIMIT'
                            WHEN EXISTS (SELECT 1 FROM spans s WHERE s.trace_id = t.id AND COALESCE(s.error, '') ILIKE '%%context%%') THEN 'CONTEXT_OVERFLOW'
                            WHEN EXISTS (SELECT 1 FROM spans s WHERE s.trace_id = t.id AND COALESCE(s.error, '') ILIKE '%%retriev%%') THEN 'RETRIEVAL_NULL'
                            ELSE 'FAILED_UNKNOWN'
                        END AS fp
                    FROM traces t
                    WHERE t.agent_id = :agentId
                      AND t.started_at >= :from
                      AND t.started_at < :to
                      AND t.status = 'FAILED'
                      """ + modelClause + """
                )
                SELECT day::text, fp, COUNT(*)
                FROM failed
                GROUP BY day, fp
                ORDER BY day ASC, COUNT(*) DESC
                LIMIT 200
                """;

        Query q = em.createNativeQuery(sql);
        bindCommon(q, agentId, from, to, model);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<AnalyticsDto.FailureFingerprintTrend> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(new AnalyticsDto.FailureFingerprintTrend(
                    row[0].toString(),
                    (String) row[1],
                    ((Number) row[2]).intValue()
            ));
        }
        return result;
    }

    private AnalyticsDto.OTelCompliance getOtelCompliance(UUID agentId, LocalDate from, LocalDate to, String model) {
        String modelClause = model != null ? "AND t.metadata->>'model' = :model" : "";
        String sql = """
                SELECT
                    COUNT(*) AS total,
                    COUNT(*) FILTER (
                        WHERE EXISTS (
                            SELECT 1 FROM spans s
                            WHERE s.trace_id = t.id
                              AND s.type = 'LLM_CALL'
                              AND s.model IS NOT NULL
                              AND s.input_tokens IS NOT NULL
                              AND s.output_tokens IS NOT NULL
                        )
                        AND t.total_tokens IS NOT NULL
                    ) AS compliant
                FROM traces t
                WHERE t.agent_id = :agentId
                  AND t.started_at >= :from
                  AND t.started_at < :to
                  """ + modelClause;

        Query q = em.createNativeQuery(sql);
        bindCommon(q, agentId, from, to, model);
        Object[] row = (Object[]) q.getSingleResult();
        int total = ((Number) row[0]).intValue();
        int compliant = ((Number) row[1]).intValue();
        double rate = total == 0 ? 0.0 : (double) compliant / total;
        return new AnalyticsDto.OTelCompliance(total, compliant, rate);
    }

    private AnalyticsDto.EvalLoopSummary getEvalLoopSummary(UUID agentId, LocalDate from, LocalDate to, String model) {
        String modelClause = model != null ? "AND t.metadata->>'model' = :model" : "";
        String sql = """
                SELECT
                    COUNT(DISTINCT t.id) AS total_traces,
                    COUNT(DISTINCT s.trace_id) AS evaluated_traces,
                    AVG(s.numeric_value) FILTER (WHERE s.name = 'auto_quality_v1') AS avg_quality
                FROM traces t
                LEFT JOIN scores s ON s.trace_id = t.id AND s.name = 'auto_quality_v1'
                WHERE t.agent_id = :agentId
                  AND t.started_at >= :from
                  AND t.started_at < :to
                  """ + modelClause;

        Query q = em.createNativeQuery(sql);
        bindCommon(q, agentId, from, to, model);
        Object[] row = (Object[]) q.getSingleResult();
        int total = ((Number) row[0]).intValue();
        int evaluated = ((Number) row[1]).intValue();
        Double avg = row[2] == null ? null : ((Number) row[2]).doubleValue();
        double coverage = total == 0 ? 0.0 : (double) evaluated / total;
        return new AnalyticsDto.EvalLoopSummary(total, evaluated, coverage, avg);
    }

    private AnalyticsDto.LearningVelocity getLearningVelocity(
            UUID agentId,
            LocalDate from,
            LocalDate to,
            String model,
            List<AnalyticsDto.FailureFingerprint> fps
    ) {
        String modelClause = model != null ? "AND t.metadata->>'model' = :model" : "";

        Query costQ = em.createNativeQuery("""
                SELECT
                    COALESCE(SUM(t.total_cost) FILTER (WHERE t.status = 'COMPLETED'), 0),
                    COUNT(*) FILTER (WHERE t.status = 'COMPLETED')
                FROM traces t
                WHERE t.agent_id = :agentId
                  AND t.started_at >= :from
                  AND t.started_at < :to
                """ + modelClause);
        bindCommon(costQ, agentId, from, to, model);
        Object[] costRow = (Object[]) costQ.getSingleResult();
        double completedCost = ((Number) costRow[0]).doubleValue();
        int completedCount = ((Number) costRow[1]).intValue();
        double costPerSuccess = completedCount == 0 ? 0.0 : completedCost / completedCount;

        Query memQ = em.createNativeQuery("""
                SELECT
                    COUNT(DISTINCT t.id),
                    COUNT(DISTINCT t.id) FILTER (WHERE EXISTS (
                        SELECT 1 FROM spans s WHERE s.trace_id = t.id AND s.type = 'RETRIEVAL'
                    ))
                FROM traces t
                WHERE t.agent_id = :agentId
                  AND t.started_at >= :from
                  AND t.started_at < :to
                """ + modelClause);
        bindCommon(memQ, agentId, from, to, model);
        Object[] memRow = (Object[]) memQ.getSingleResult();
        int total = ((Number) memRow[0]).intValue();
        int withMemory = ((Number) memRow[1]).intValue();
        double memoryHitRate = total == 0 ? 0.0 : (double) withMemory / total;

        double repeatFailureRate = 0.0;
        int failedTotal = fps.stream().mapToInt(AnalyticsDto.FailureFingerprint::count).sum();
        int repeatedFailures = fps.stream().filter(f -> f.count() > 1).mapToInt(AnalyticsDto.FailureFingerprint::count).sum();
        if (failedTotal > 0) repeatFailureRate = (double) repeatedFailures / failedTotal;

        Query recoveryQ = em.createNativeQuery("""
                SELECT status::text, started_at
                FROM traces t
                WHERE t.agent_id = :agentId
                  AND t.started_at >= :from
                  AND t.started_at < :to
                """ + modelClause + " ORDER BY started_at ASC");
        bindCommon(recoveryQ, agentId, from, to, model);
        List<Object[]> ordered = recoveryQ.getResultList();
        List<Double> recoveries = new ArrayList<>();
        OffsetDateTime pendingFailedAt = null;
        for (Object[] row : ordered) {
            String status = (String) row[0];
            OffsetDateTime startedAt;
            Object startedRaw = row[1];
            if (startedRaw instanceof java.time.Instant instant) {
                startedAt = instant.atOffset(java.time.ZoneOffset.UTC);
            } else if (startedRaw instanceof java.sql.Timestamp ts) {
                startedAt = ts.toInstant().atOffset(java.time.ZoneOffset.UTC);
            } else if (startedRaw instanceof OffsetDateTime odt) {
                startedAt = odt;
            } else {
                startedAt = OffsetDateTime.parse(startedRaw.toString());
            }
            if ("FAILED".equals(status) && pendingFailedAt == null) {
                pendingFailedAt = startedAt;
            } else if ("COMPLETED".equals(status) && pendingFailedAt != null) {
                recoveries.add((double) Duration.between(pendingFailedAt, startedAt).toMinutes());
                pendingFailedAt = null;
            }
        }
        double meanRecovery = recoveries.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        return new AnalyticsDto.LearningVelocity(costPerSuccess, repeatFailureRate, memoryHitRate, meanRecovery);
    }

    private AnalyticsDto.CostTrend getCostTrend(UUID agentId, LocalDate from, LocalDate to, String model) {
        long daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        if (daysInPeriod <= 0) daysInPeriod = 1;

        double currentCost = sumCost(agentId, from, to, model);
        LocalDate prevFrom = from.minusDays(daysInPeriod);
        LocalDate prevTo = from.minusDays(1);
        double previousCost = sumCost(agentId, prevFrom, prevTo, model);

        double percentChange = previousCost == 0.0
                ? (currentCost > 0 ? 100.0 : 0.0)
                : ((currentCost - previousCost) / previousCost) * 100.0;

        double projectedMonthlyCost = (currentCost / daysInPeriod) * 30.0;
        return new AnalyticsDto.CostTrend(percentChange, projectedMonthlyCost);
    }

    private double sumCost(UUID agentId, LocalDate from, LocalDate to, String model) {
        String modelClause = model != null ? "AND t.metadata->>'model' = :model" : "";
        String sql = """
                SELECT COALESCE(SUM(t.total_cost), 0)
                FROM traces t
                WHERE t.agent_id = :agentId
                  AND t.started_at >= :from
                  AND t.started_at < :to
                  """ + modelClause;
        Query q = em.createNativeQuery(sql);
        bindCommon(q, agentId, from, to, model);
        Object result = q.getSingleResult();
        return result == null ? 0.0 : ((Number) result).doubleValue();
    }

    private void bindCommon(Query q, UUID agentId, LocalDate from, LocalDate to, String model) {
        q.setParameter("agentId", agentId);
        q.setParameter("from", java.sql.Date.valueOf(from));
        q.setParameter("to", java.sql.Date.valueOf(to.plusDays(1)));
        if (model != null) q.setParameter("model", model);
    }
}

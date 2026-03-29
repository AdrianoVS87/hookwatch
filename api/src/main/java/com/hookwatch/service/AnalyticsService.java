package com.hookwatch.service;

import com.hookwatch.dto.AnalyticsDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final EntityManager em;

    /**
     * Returns analytics for the given agent scoped to the date range [from, to].
     * granularity is accepted but currently only "day" is implemented (date_trunc('day', ...)).
     * model: optional filter — when non-null, only traces with metadata->>'model' = model are included.
     */
    @Transactional(readOnly = true)
    public AnalyticsDto getAnalytics(UUID agentId, LocalDate from, LocalDate to, String granularity, String model) {
        List<AnalyticsDto.DailyUsage> dailyUsage = getDailyUsage(agentId, from, to, model);
        List<AnalyticsDto.ModelUsage> byModel = getModelUsage(agentId, from, to, model);
        List<AnalyticsDto.TopTrace> topTraces = getTopExpensiveTraces(agentId, from, to, model);
        AnalyticsDto.CostTrend costTrend = getCostTrend(agentId, from, to, model);

        return new AnalyticsDto(dailyUsage, byModel, topTraces, costTrend);
    }

    @SuppressWarnings("unchecked")
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
        q.setParameter("agentId", agentId);
        q.setParameter("from", java.sql.Date.valueOf(from));
        q.setParameter("to", java.sql.Date.valueOf(to.plusDays(1)));
        if (model != null) q.setParameter("model", model);

        List<Object[]> rows = q.getResultList();
        List<AnalyticsDto.DailyUsage> result = new ArrayList<>();
        for (Object[] row : rows) {
            String date = row[0].toString();
            int totalTokens = ((Number) row[1]).intValue();
            double totalCost = ((Number) row[2]).doubleValue();
            int traceCount = ((Number) row[3]).intValue();
            double avgLatencyMs = ((Number) row[4]).doubleValue();
            double errorRate = ((Number) row[5]).doubleValue();
            result.add(new AnalyticsDto.DailyUsage(date, totalTokens, totalCost, traceCount, avgLatencyMs, errorRate));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
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
        q.setParameter("agentId", agentId);
        q.setParameter("from", java.sql.Date.valueOf(from));
        q.setParameter("to", java.sql.Date.valueOf(to.plusDays(1)));
        if (model != null) q.setParameter("model", model);

        List<Object[]> rows = q.getResultList();
        List<AnalyticsDto.ModelUsage> result = new ArrayList<>();
        for (Object[] row : rows) {
            String modelName = (String) row[0];
            int totalTokens = ((Number) row[1]).intValue();
            double totalCost = ((Number) row[2]).doubleValue();
            int traceCount = ((Number) row[3]).intValue();
            result.add(new AnalyticsDto.ModelUsage(modelName, totalTokens, totalCost, traceCount));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
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
        q.setParameter("agentId", agentId);
        q.setParameter("from", java.sql.Date.valueOf(from));
        q.setParameter("to", java.sql.Date.valueOf(to.plusDays(1)));
        if (model != null) q.setParameter("model", model);

        List<Object[]> rows = q.getResultList();
        List<AnalyticsDto.TopTrace> result = new ArrayList<>();
        for (Object[] row : rows) {
            String traceId = (String) row[0];
            double totalCost = ((Number) row[1]).doubleValue();
            int totalTokens = ((Number) row[2]).intValue();
            String startedAt = row[3].toString();
            result.add(new AnalyticsDto.TopTrace(traceId, totalCost, totalTokens, startedAt));
        }
        return result;
    }

    /**
     * Computes cost trend:
     * - percentChangeVsPreviousPeriod: (currentCost - previousCost) / previousCost * 100
     * - projectedMonthlyCost: (currentPeriodCost / daysInPeriod) * 30
     */
    private AnalyticsDto.CostTrend getCostTrend(UUID agentId, LocalDate from, LocalDate to, String model) {
        long daysInPeriod = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        if (daysInPeriod <= 0) daysInPeriod = 1;

        double currentCost = sumCost(agentId, from, to, model);
        LocalDate prevFrom = from.minusDays(daysInPeriod);
        LocalDate prevTo = from.minusDays(1);
        double previousCost = sumCost(agentId, prevFrom, prevTo, model);

        double percentChange;
        if (previousCost == 0.0) {
            percentChange = currentCost > 0 ? 100.0 : 0.0;
        } else {
            percentChange = ((currentCost - previousCost) / previousCost) * 100.0;
        }

        double projectedMonthlyCost = (currentCost / daysInPeriod) * 30.0;

        return new AnalyticsDto.CostTrend(percentChange, projectedMonthlyCost);
    }

    @SuppressWarnings("unchecked")
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
        q.setParameter("agentId", agentId);
        q.setParameter("from", java.sql.Date.valueOf(from));
        q.setParameter("to", java.sql.Date.valueOf(to.plusDays(1)));
        if (model != null) q.setParameter("model", model);
        Object result = q.getSingleResult();
        return result == null ? 0.0 : ((Number) result).doubleValue();
    }
}

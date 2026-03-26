package com.hookwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class AnalyticsIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String apiKey;
    private UUID agentId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        String[] keyAndId = createTenantAndGetKeyAndId("analytics-tenant-" + UUID.randomUUID());
        apiKey = keyAndId[0];
        tenantId = keyAndId[1];
        agentId = createAgent(apiKey, tenantId, "analytics-agent");
    }

    // ── Daily Usage Tests ─────────────────────────────────────────────────────

    @Test
    void analytics_shouldReturnDailyUsageWithCorrectDatesAndCounts() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        createTraceOnDate(agentId, yesterday, 500, 0.005, "COMPLETED");
        createTraceOnDate(agentId, yesterday, 300, 0.003, "COMPLETED");
        createTraceOnDate(agentId, today, 700, 0.007, "FAILED");

        Map<String, Object> analytics = fetchAnalytics(agentId, yesterday, today);
        List<Map> dailyUsage = (List<Map>) analytics.get("dailyUsage");

        assertThat(dailyUsage).isNotNull();
        assertThat(dailyUsage).hasSize(2);

        Map yesterdayEntry = dailyUsage.stream()
                .filter(d -> d.get("date").toString().startsWith(yesterday.toString()))
                .findFirst().orElseThrow(() -> new AssertionError("No entry for " + yesterday));
        assertThat(((Number) yesterdayEntry.get("traceCount")).intValue()).isEqualTo(2);
        assertThat(((Number) yesterdayEntry.get("totalTokens")).intValue()).isEqualTo(800);

        Map todayEntry = dailyUsage.stream()
                .filter(d -> d.get("date").toString().startsWith(today.toString()))
                .findFirst().orElseThrow(() -> new AssertionError("No entry for " + today));
        assertThat(((Number) todayEntry.get("traceCount")).intValue()).isEqualTo(1);
        assertThat(((Number) todayEntry.get("errorRate")).doubleValue()).isEqualTo(1.0);
    }

    @Test
    void analytics_shouldReturnCorrectErrorRate() {
        LocalDate today = LocalDate.now();

        createTraceOnDate(agentId, today, 100, 0.001, "COMPLETED");
        createTraceOnDate(agentId, today, 200, 0.002, "COMPLETED");
        createTraceOnDate(agentId, today, 300, 0.003, "FAILED");
        createTraceOnDate(agentId, today, 400, 0.004, "FAILED");

        Map<String, Object> analytics = fetchAnalytics(agentId, today, today);
        List<Map> dailyUsage = (List<Map>) analytics.get("dailyUsage");

        assertThat(dailyUsage).hasSize(1);
        double errorRate = ((Number) dailyUsage.get(0).get("errorRate")).doubleValue();
        assertThat(errorRate).isEqualTo(0.5);
    }

    // ── Model Aggregation Tests ───────────────────────────────────────────────

    @Test
    void analytics_shouldReturnByModelAggregation() {
        LocalDate today = LocalDate.now();

        createTraceWithModelSpanOnDate(agentId, today, "gpt-4", 500, 0.01);
        createTraceWithModelSpanOnDate(agentId, today, "gpt-4", 300, 0.008);
        createTraceWithModelSpanOnDate(agentId, today, "gpt-3.5-turbo", 200, 0.001);

        Map<String, Object> analytics = fetchAnalytics(agentId, today, today);
        List<Map> byModel = (List<Map>) analytics.get("byModel");

        assertThat(byModel).isNotNull();
        assertThat(byModel).hasSizeGreaterThanOrEqualTo(2);

        Map gpt4Entry = byModel.stream()
                .filter(m -> "gpt-4".equals(m.get("model")))
                .findFirst().orElseThrow();
        assertThat(((Number) gpt4Entry.get("traceCount")).intValue()).isEqualTo(2);
        assertThat(((Number) gpt4Entry.get("totalTokens")).intValue()).isGreaterThan(0);
    }

    // ── Top Expensive Traces Tests ────────────────────────────────────────────

    @Test
    void analytics_shouldReturnTopExpensiveTracesSortedByDescCost() {
        LocalDate today = LocalDate.now();

        createTraceOnDate(agentId, today, 100, 0.01, "COMPLETED");
        createTraceOnDate(agentId, today, 100, 0.05, "COMPLETED");
        createTraceOnDate(agentId, today, 100, 0.03, "COMPLETED");
        createTraceOnDate(agentId, today, 100, 0.10, "COMPLETED");
        createTraceOnDate(agentId, today, 100, 0.02, "COMPLETED");

        Map<String, Object> analytics = fetchAnalytics(agentId, today, today);
        List<Map> topTraces = (List<Map>) analytics.get("topExpensiveTraces");

        assertThat(topTraces).isNotNull();
        assertThat(topTraces).hasSizeGreaterThanOrEqualTo(5);

        // Verify descending order
        for (int i = 0; i < topTraces.size() - 1; i++) {
            double cost1 = ((Number) topTraces.get(i).get("totalCost")).doubleValue();
            double cost2 = ((Number) topTraces.get(i + 1).get("totalCost")).doubleValue();
            assertThat(cost1).isGreaterThanOrEqualTo(cost2);
        }
    }

    @Test
    void analytics_topExpensiveTraces_shouldHaveRequiredFields() {
        LocalDate today = LocalDate.now();
        createTraceOnDate(agentId, today, 1000, 0.05, "COMPLETED");

        Map<String, Object> analytics = fetchAnalytics(agentId, today, today);
        List<Map> topTraces = (List<Map>) analytics.get("topExpensiveTraces");

        assertThat(topTraces).isNotEmpty();
        Map trace = topTraces.get(0);
        assertThat(trace.get("traceId")).isNotNull();
        assertThat(trace.get("totalCost")).isNotNull();
        assertThat(trace.get("totalTokens")).isNotNull();
        assertThat(trace.get("startedAt")).isNotNull();
    }

    // ── Cost Trend Tests ──────────────────────────────────────────────────────

    @Test
    void analytics_shouldCalculateProjectedMonthlyCostCorrectly() {
        LocalDate today = LocalDate.now();
        createTraceOnDate(agentId, today, 100, 0.10, "COMPLETED");

        Map<String, Object> analytics = fetchAnalytics(agentId, today, today);
        Map costTrend = (Map) analytics.get("costTrend");

        assertThat(costTrend).isNotNull();
        double projected = ((Number) costTrend.get("projectedMonthlyCost")).doubleValue();
        // 1 day at 0.10 cost → projected = 0.10 * 30 = 3.0
        assertThat(projected).isCloseTo(3.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void analytics_costTrend_shouldHandleZeroPreviousPeriod() {
        LocalDate today = LocalDate.now();
        createTraceOnDate(agentId, today, 100, 0.10, "COMPLETED");

        Map<String, Object> analytics = fetchAnalytics(agentId, today, today);
        Map costTrend = (Map) analytics.get("costTrend");

        assertThat(costTrend).isNotNull();
        double percentChange = ((Number) costTrend.get("percentChangeVsPreviousPeriod")).doubleValue();
        // When previous period has no data, should return 100.0
        assertThat(percentChange).isEqualTo(100.0);
    }

    // ── Security Tests ────────────────────────────────────────────────────────

    @Test
    void analytics_shouldReturn403ForCrossTenantAgentAccess() {
        String otherApiKey = createTenantAndGetKey("analytics-other-" + UUID.randomUUID());

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/analytics?agentId=" + agentId + "&from=2024-01-01&to=2024-01-31",
                HttpMethod.GET, new HttpEntity<>(authHeaders(otherApiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void analytics_shouldReturn400WhenFromIsAfterTo() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/analytics?agentId=" + agentId + "&from=2024-12-31&to=2024-01-01",
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> fetchAnalytics(UUID agentId, LocalDate from, LocalDate to) {
        String url = baseUrl() + "/analytics?agentId=" + agentId
                + "&from=" + from.format(DateTimeFormatter.ISO_DATE)
                + "&to=" + to.format(DateTimeFormatter.ISO_DATE);

        ResponseEntity<Map> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    /**
     * Creates a trace and overrides its started_at in DB to simulate historical data.
     */
    private void createTraceOnDate(UUID agentId, LocalDate date, int tokens, double cost, String status) {
        UUID traceId = createTrace(apiKey, agentId, status, tokens, String.valueOf(cost));
        jdbcTemplate.update(
                "UPDATE traces SET started_at = ?, completed_at = ? WHERE id = ?",
                java.sql.Timestamp.valueOf(date.atStartOfDay().plusHours(12)),
                java.sql.Timestamp.valueOf(date.atStartOfDay().plusHours(13)),
                traceId
        );
    }

    /**
     * Creates a trace with a single span having a specific model, then overrides the date.
     */
    private void createTraceWithModelSpanOnDate(UUID agentId, LocalDate date, String model,
                                                 int tokens, double cost) {
        String traceJson = """
                {
                    "agentId": "%s",
                    "status": "COMPLETED",
                    "totalTokens": %d,
                    "totalCost": %s,
                    "spans": [
                        {
                            "name": "llm-call",
                            "type": "LLM_CALL",
                            "status": "COMPLETED",
                            "inputTokens": %d,
                            "outputTokens": %d,
                            "cost": %s,
                            "model": "%s",
                            "sortOrder": 0
                        }
                    ]
                }
                """.formatted(agentId, tokens, cost, tokens / 2, tokens / 2, cost, model);

        HttpEntity<String> entity = new HttpEntity<>(traceJson, authHeaders(apiKey));
        ResponseEntity<Map> response = restTemplate.postForEntity(baseUrl() + "/traces", entity, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        UUID traceId = UUID.fromString((String) response.getBody().get("id"));
        jdbcTemplate.update(
                "UPDATE traces SET started_at = ?, completed_at = ? WHERE id = ?",
                java.sql.Timestamp.valueOf(date.atStartOfDay().plusHours(10)),
                java.sql.Timestamp.valueOf(date.atStartOfDay().plusHours(11)),
                traceId
        );
    }
}

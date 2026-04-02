package com.hookwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class ThetaReadinessIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String apiKey;
    private String tenantId;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        String[] keyAndId = createTenantAndGetKeyAndId("theta-tenant-" + UUID.randomUUID());
        apiKey = keyAndId[0];
        tenantId = keyAndId[1];
        agentId = createAgent(apiKey, tenantId, "theta-agent");
    }

    @Test
    void shouldRunAutoEvalAndComputeLearningVelocityFromKnownPoints() {
        LocalDate base = LocalDate.now().minusDays(1);

        String t1 = ingestTrace("FAILED", 1000, 1.0, false, "Connection timeout");
        String t2 = ingestTrace("COMPLETED", 1000, 1.0, false, null);
        String t3 = ingestTrace("COMPLETED", 1000, 2.0, true, null);
        String t4 = ingestTrace("FAILED", 1000, 1.0, false, "Connection timeout");
        String t5 = ingestTrace("COMPLETED", 1000, 3.0, false, null);

        // deterministic ordering for mean recovery: failed@00:00 -> completed@00:10; failed@01:00 -> completed@01:30
        setTraceStart(t1, base.atTime(0, 0));
        setTraceStart(t2, base.atTime(0, 10));
        setTraceStart(t3, base.atTime(0, 20));
        setTraceStart(t4, base.atTime(1, 0));
        setTraceStart(t5, base.atTime(1, 30));

        ResponseEntity<Map> autoEval = restTemplate.exchange(
                baseUrl() + "/agents/" + agentId + "/scores/auto-evaluate?limit=50",
                HttpMethod.POST,
                new HttpEntity<>(authHeaders(apiKey)),
                Map.class
        );

        assertThat(autoEval.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> evalBody = autoEval.getBody();
        assertThat(evalBody).isNotNull();
        assertThat(((Number) evalBody.get("evaluatedNow")).intValue()).isGreaterThan(0);

        LocalDate from = base.minusDays(1);
        LocalDate to = base.plusDays(1);
        ResponseEntity<Map> analyticsResp = restTemplate.exchange(
                baseUrl() + "/analytics?agentId=" + agentId + "&from=" + from + "&to=" + to,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(apiKey)),
                Map.class
        );

        assertThat(analyticsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> analytics = analyticsResp.getBody();
        assertThat(analytics).isNotNull();

        Map<String, Object> lv = (Map<String, Object>) analytics.get("learningVelocity");
        assertThat(lv).isNotNull();

        // Completed costs = 1 + 2 + 3, completed count = 3 => 2.0
        assertThat(((Number) lv.get("costPerSuccessfulTrace")).doubleValue()).isEqualTo(2.0);
        // failures are same category and repeated => 2/2 = 1.0
        assertThat(((Number) lv.get("repeatFailureRate")).doubleValue()).isEqualTo(1.0);
        // one retrieval trace out of five traces => 0.2
        assertThat(((Number) lv.get("memoryHitRate")).doubleValue()).isEqualTo(0.2);
        // mean recovery = (10 + 30) / 2 = 20 minutes
        assertThat(((Number) lv.get("meanRecoveryMinutes")).doubleValue()).isEqualTo(20.0);

        Map<String, Object> evalLoopSummary = (Map<String, Object>) analytics.get("evalLoopSummary");
        assertThat(evalLoopSummary).isNotNull();
        assertThat(((Number) evalLoopSummary.get("evaluatedTraces")).intValue()).isGreaterThan(0);
        assertThat(((Number) evalLoopSummary.get("evaluationCoverage")).doubleValue()).isGreaterThan(0.0);
    }

    private String ingestTrace(String status, int tokens, double cost, boolean includeRetrieval, String error) {
        String retrievalSpan = includeRetrieval ? """
                ,
                {
                  "name": "kb-search",
                  "type": "RETRIEVAL",
                  "status": "COMPLETED",
                  "sortOrder": 1
                }
                """ : "";

        String maybeError = error != null ? "\"error\": \"" + error + "\"," : "";

        String payload = """
                {
                  "agentId": "%s",
                  "status": "%s",
                  "totalTokens": %d,
                  "totalCost": %.3f,
                  "spans": [
                    {
                      "name": "llm-call",
                      "type": "LLM_CALL",
                      "status": "%s",
                      "model": "claude-sonnet-4-6",
                      "inputTokens": 700,
                      "outputTokens": 300,
                      %s
                      "sortOrder": 0
                    }
                    %s
                  ]
                }
                """.formatted(agentId, status, tokens, cost, status, maybeError, retrievalSpan);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces",
                new HttpEntity<>(payload, authHeaders(apiKey)),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return String.valueOf(response.getBody().get("id"));
    }

    private void setTraceStart(String traceId, LocalDateTime start) {
        jdbcTemplate.update(
                "UPDATE traces SET started_at = ?, completed_at = ? WHERE id = ?::uuid",
                java.sql.Timestamp.valueOf(start),
                java.sql.Timestamp.valueOf(start.plusMinutes(1)),
                traceId
        );
    }
}

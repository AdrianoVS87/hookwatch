package com.hookwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class FingerprintIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String apiKey;
    private String tenantId;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        String[] keyAndId = createTenantAndGetKeyAndId("fingerprint-tenant-" + UUID.randomUUID());
        apiKey = keyAndId[0];
        tenantId = keyAndId[1];
        agentId = createAgent(apiKey, tenantId, "fingerprint-agent");
    }

    @Test
    void shouldGroupRecurringFailuresAndReturnTrend() {
        LocalDate base = LocalDate.now().minusDays(3);

        ingestFailedTrace(base, "Connection timeout after 30s", "claude-opus-4");
        ingestFailedTrace(base, "Connection timeout after 30s", "claude-opus-4");
        ingestFailedTrace(base.plusDays(1), "Connection timeout after 30s", "claude-opus-4");
        ingestFailedTrace(base.plusDays(2), "Connection timeout after 30s", "claude-opus-4");

        ingestFailedTrace(base, "HTTP 429 rate limit", "claude-sonnet-4-6");
        ingestFailedTrace(base.plusDays(1), "HTTP 429 rate limit", "claude-sonnet-4-6");
        ingestFailedTrace(base.plusDays(2), "HTTP 429 rate limit", "claude-sonnet-4-6");

        ingestFailedTrace(base, "Context length exceeded", "codex-5.3");
        ingestFailedTrace(base.plusDays(1), "Context length exceeded", "codex-5.3");
        ingestFailedTrace(base.plusDays(2), "Context length exceeded", "codex-5.3");

        ResponseEntity<List> listResponse = restTemplate.exchange(
                baseUrl() + "/fingerprints?agentId=" + agentId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(apiKey)),
                List.class
        );

        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> rows = listResponse.getBody();
        assertThat(rows).isNotNull();
        assertThat(rows).hasSize(3);

        Map<String, Object> timeout = rows.stream()
                .filter(r -> String.valueOf(r.get("errorMessage")).contains("timeout"))
                .findFirst()
                .orElseThrow();

        assertThat(((Number) timeout.get("occurrenceCount")).intValue()).isEqualTo(4);

        String timeoutId = String.valueOf(timeout.get("id"));
        String from = base.toString();
        String to = base.plusDays(2).toString();

        ResponseEntity<Map> trendResponse = restTemplate.exchange(
                baseUrl() + "/fingerprints/" + timeoutId + "/trend?from=" + from + "&to=" + to,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(apiKey)),
                Map.class
        );

        assertThat(trendResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> trendBody = trendResponse.getBody();
        assertThat(trendBody).isNotNull();

        List<Map<String, Object>> trend = (List<Map<String, Object>>) trendBody.get("trend");
        assertThat(trend).isNotNull();
        assertThat(trend).hasSize(3);

        Map<String, Integer> byDay = trend.stream().collect(java.util.stream.Collectors.toMap(
                t -> String.valueOf(t.get("date")),
                t -> ((Number) t.get("count")).intValue()
        ));

        assertThat(byDay.get(base.toString())).isEqualTo(2);
        assertThat(byDay.get(base.plusDays(1).toString())).isEqualTo(1);
        assertThat(byDay.get(base.plusDays(2).toString())).isEqualTo(1);
    }

    private void ingestFailedTrace(LocalDate date, String error, String model) {
        String payload = """
                {
                  "agentId": "%s",
                  "status": "FAILED",
                  "totalTokens": 1500,
                  "totalCost": 0.03,
                  "spans": [
                    {
                      "name": "llm-call",
                      "type": "LLM_CALL",
                      "status": "FAILED",
                      "model": "%s",
                      "inputTokens": 1000,
                      "outputTokens": 500,
                      "error": "%s",
                      "sortOrder": 0
                    }
                  ]
                }
                """.formatted(agentId, model, error);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces",
                new HttpEntity<>(payload, authHeaders(apiKey)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String traceId = String.valueOf(response.getBody().get("id"));

        jdbcTemplate.update(
                "UPDATE traces SET started_at = ?, completed_at = ? WHERE id = ?::uuid",
                java.sql.Timestamp.valueOf(date.atStartOfDay().plusHours(12)),
                java.sql.Timestamp.valueOf(date.atStartOfDay().plusHours(12).plusMinutes(1)),
                traceId
        );
    }
}

package com.hookwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class OtelComplianceIntegrationTest extends BaseIntegrationTest {

    private String apiKey;
    private String tenantId;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        String[] keyAndId = createTenantAndGetKeyAndId("otel-tenant-" + UUID.randomUUID());
        apiKey = keyAndId[0];
        tenantId = keyAndId[1];
        agentId = createAgent(apiKey, tenantId, "otel-agent");
    }

    @Test
    void shouldReturn100PercentForValidTrace() {
        String traceId = ingestTrace("""
                {
                  "agentId": "%s",
                  "status": "COMPLETED",
                  "totalTokens": 1500,
                  "totalCost": 0.04,
                  "metadata": {
                    "traceparent": "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                    "service.name": "hookwatch-api",
                    "service.version": "1.0.0"
                  },
                  "spans": [
                    {
                      "name": "llm-call",
                      "type": "LLM_CALL",
                      "status": "COMPLETED",
                      "model": "claude-sonnet-4-6",
                      "inputTokens": 1000,
                      "outputTokens": 500,
                      "sortOrder": 0
                    }
                  ]
                }
                """.formatted(agentId));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/" + traceId + "/compliance",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(apiKey)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> report = response.getBody();
        assertThat(report).isNotNull();
        assertThat(((Number) report.get("failed")).intValue()).isEqualTo(0);
        assertThat(((Number) report.get("passed")).intValue()).isEqualTo(((Number) report.get("totalChecks")).intValue());
    }

    @Test
    void shouldDetectMissingServiceNameGap() {
        String traceId = ingestTrace("""
                {
                  "agentId": "%s",
                  "status": "COMPLETED",
                  "totalTokens": 1200,
                  "totalCost": 0.03,
                  "metadata": {
                    "traceparent": "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01",
                    "service.version": "1.0.0"
                  },
                  "spans": [
                    {
                      "name": "llm-call",
                      "type": "LLM_CALL",
                      "status": "COMPLETED",
                      "model": "codex-5.3",
                      "inputTokens": 800,
                      "outputTokens": 400,
                      "sortOrder": 0
                    }
                  ]
                }
                """.formatted(agentId));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/" + traceId + "/compliance",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(apiKey)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> report = response.getBody();
        assertThat(report).isNotNull();
        assertThat(((Number) report.get("failed")).intValue()).isGreaterThan(0);

        List<Map<String, Object>> gaps = (List<Map<String, Object>>) report.get("gaps");
        assertThat(gaps).anyMatch(g -> "resource.service.name".equals(g.get("field")));
    }

    @Test
    void shouldDetectMalformedTraceparent() {
        String traceId = ingestTrace("""
                {
                  "agentId": "%s",
                  "status": "COMPLETED",
                  "totalTokens": 900,
                  "totalCost": 0.02,
                  "metadata": {
                    "traceparent": "malformed-traceparent",
                    "service.name": "hookwatch-api",
                    "service.version": "1.0.0"
                  },
                  "spans": [
                    {
                      "name": "llm-call",
                      "type": "LLM_CALL",
                      "status": "COMPLETED",
                      "model": "claude-opus-4-6",
                      "inputTokens": 500,
                      "outputTokens": 400,
                      "sortOrder": 0
                    }
                  ]
                }
                """.formatted(agentId));

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/" + traceId + "/compliance",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(apiKey)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> report = response.getBody();
        assertThat(report).isNotNull();

        List<Map<String, Object>> gaps = (List<Map<String, Object>>) report.get("gaps");
        assertThat(gaps).anyMatch(g -> "metadata.traceparent".equals(g.get("field")));
    }

    private String ingestTrace(String payload) {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces",
                new HttpEntity<>(payload, authHeaders(apiKey)),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return String.valueOf(response.getBody().get("id"));
    }
}

package com.hookwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreIntegrationTest extends BaseIntegrationTest {

    private String apiKey;
    private String tenantId;
    private UUID agentId;
    private UUID traceId;

    @BeforeEach
    void setUp() {
        String[] keyAndId = createTenantAndGetKeyAndId("score-tenant-" + UUID.randomUUID());
        apiKey = keyAndId[0];
        tenantId = keyAndId[1];
        agentId = createAgent(apiKey, tenantId, "score-agent");
        traceId = createTrace(apiKey, agentId, "COMPLETED", 500, "0.005");
    }

    @Test
    void shouldCreateNumericScore() {
        String json = """
            {
                "name": "accuracy",
                "dataType": "NUMERIC",
                "numericValue": 0.85,
                "source": "API",
                "comment": "High accuracy"
            }
            """;
        HttpEntity<String> entity = new HttpEntity<>(json, authHeaders(apiKey));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces/" + traceId + "/scores", entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("name")).isEqualTo("accuracy");
        assertThat(((Number) response.getBody().get("numericValue")).doubleValue()).isEqualTo(0.85);
    }

    @Test
    void shouldCreateCategoricalScore() {
        String json = """
            {
                "name": "quality",
                "dataType": "CATEGORICAL",
                "stringValue": "good",
                "source": "MANUAL"
            }
            """;
        HttpEntity<String> entity = new HttpEntity<>(json, authHeaders(apiKey));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces/" + traceId + "/scores", entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("stringValue")).isEqualTo("good");
    }

    @Test
    void shouldCreateBooleanScore() {
        String json = """
            {
                "name": "hallucination_free",
                "dataType": "BOOLEAN",
                "booleanValue": true,
                "source": "LLM_JUDGE"
            }
            """;
        HttpEntity<String> entity = new HttpEntity<>(json, authHeaders(apiKey));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces/" + traceId + "/scores", entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().get("booleanValue")).isEqualTo(true);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldListAllScoresForTrace() {
        // Create 3 scores
        for (String json : List.of(
            """
            {"name":"accuracy","dataType":"NUMERIC","numericValue":0.85,"source":"API"}
            """,
            """
            {"name":"quality","dataType":"CATEGORICAL","stringValue":"good","source":"MANUAL"}
            """,
            """
            {"name":"safe","dataType":"BOOLEAN","booleanValue":true,"source":"LLM_JUDGE"}
            """
        )) {
            HttpEntity<String> entity = new HttpEntity<>(json, authHeaders(apiKey));
            restTemplate.postForEntity(baseUrl() + "/traces/" + traceId + "/scores", entity, Map.class);
        }

        ResponseEntity<List> response = restTemplate.exchange(
                baseUrl() + "/traces/" + traceId + "/scores",
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(3);
    }

    @Test
    void shouldReturn404ForNonExistentTrace() {
        UUID fakeTraceId = UUID.randomUUID();
        String json = """
            {"name":"test","dataType":"NUMERIC","numericValue":0.5,"source":"API"}
            """;
        HttpEntity<String> entity = new HttpEntity<>(json, authHeaders(apiKey));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces/" + fakeTraceId + "/scores", entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturn403ForOtherTenantsTrace() {
        // Create a second tenant
        String[] otherKeyAndId = createTenantAndGetKeyAndId("other-tenant-" + UUID.randomUUID());
        String otherApiKey = otherKeyAndId[0];

        String json = """
            {"name":"test","dataType":"NUMERIC","numericValue":0.5,"source":"API"}
            """;
        HttpEntity<String> entity = new HttpEntity<>(json, authHeaders(otherApiKey));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces/" + traceId + "/scores", entity, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnScoreSummaryByAgent() {
        // Create multiple traces with numeric scores
        UUID trace2 = createTrace(apiKey, agentId, "COMPLETED", 300, "0.003");

        for (UUID tid : List.of(traceId, trace2)) {
            String json = """
                {"name":"accuracy","dataType":"NUMERIC","numericValue":%s,"source":"API"}
                """.formatted(tid.equals(traceId) ? "0.80" : "0.90");
            HttpEntity<String> entity = new HttpEntity<>(json, authHeaders(apiKey));
            restTemplate.postForEntity(baseUrl() + "/traces/" + tid + "/scores", entity, Map.class);
        }

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/agents/" + agentId + "/scores/summary",
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = response.getBody();
        List<Map<String, Object>> numericAverages = (List<Map<String, Object>>) body.get("numericAverages");
        assertThat(numericAverages).hasSize(1);
        assertThat(numericAverages.get(0).get("name")).isEqualTo("accuracy");
        double avg = ((Number) numericAverages.get(0).get("average")).doubleValue();
        assertThat(avg).isEqualTo(0.85, org.assertj.core.data.Offset.offset(0.01));
    }
}

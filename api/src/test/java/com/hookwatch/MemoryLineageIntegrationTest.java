package com.hookwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class MemoryLineageIntegrationTest extends BaseIntegrationTest {

    private String apiKey;
    private String tenantId;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        String[] keyAndId = createTenantAndGetKeyAndId("lineage-tenant-" + UUID.randomUUID());
        apiKey = keyAndId[0];
        tenantId = keyAndId[1];
        agentId = createAgent(apiKey, tenantId, "lineage-agent");
    }

    @Test
    void shouldReturnLineageChainForTraceABC() {
        String traceA = createTraceWithLineage(List.of(), false, "COMPLETED");
        String traceB = createTraceWithLineage(List.of(traceA), true, "COMPLETED");
        String traceC = createTraceWithLineage(List.of(traceA, traceB), true, "COMPLETED");

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/" + traceC + "/memory-lineage",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(apiKey)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();

        assertThat(body.get("traceId")).isEqualTo(traceC);
        assertThat(((Number) body.get("retrievalSpanCount")).intValue()).isGreaterThan(0);

        List<String> memoryRefs = (List<String>) body.get("memoryReferences");
        assertThat(memoryRefs).contains(traceA, traceB);

        List<String> retrievalSpans = (List<String>) body.get("retrievalSpanNames");
        assertThat(retrievalSpans).contains("kb-retrieval");
    }

    private String createTraceWithLineage(List<String> lineage, boolean includeRetrieval, String status) {
        String lineageJson = lineage.isEmpty()
                ? "[]"
                : "[" + lineage.stream().map(s -> "\"" + s + "\"").reduce((a, b) -> a + "," + b).orElse("") + "]";

        String retrieval = includeRetrieval ? """
                ,
                {
                  "name": "kb-retrieval",
                  "type": "RETRIEVAL",
                  "status": "COMPLETED",
                  "sortOrder": 1
                }
                """ : "";

        String payload = """
                {
                  "agentId": "%s",
                  "status": "%s",
                  "totalTokens": 1200,
                  "totalCost": 0.02,
                  "metadata": {
                    "memoryLineage": %s
                  },
                  "spans": [
                    {
                      "name": "llm-call",
                      "type": "LLM_CALL",
                      "status": "COMPLETED",
                      "model": "claude-opus-4-6",
                      "inputTokens": 700,
                      "outputTokens": 500,
                      "sortOrder": 0
                    }
                    %s
                  ]
                }
                """.formatted(agentId, status, lineageJson, retrieval);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces",
                new HttpEntity<>(payload, authHeaders(apiKey)),
                Map.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return String.valueOf(response.getBody().get("id"));
    }
}

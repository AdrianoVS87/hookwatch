package com.hookwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("unchecked")
class OtelExportIntegrationTest extends BaseIntegrationTest {

    private String apiKey;
    private UUID agentId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        String[] keyAndId = createTenantAndGetKeyAndId("otel-tenant-" + UUID.randomUUID());
        apiKey = keyAndId[0];
        tenantId = keyAndId[1];
        agentId = createAgent(apiKey, tenantId, "otel-agent");
    }

    // ── Export Tests ─────────────────────────────────────────────────────────

    @Test
    void exportOtel_shouldReturnHexTraceId() {
        UUID traceId = createTraceWithSpans();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/" + traceId + "/otel",
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = response.getBody();
        assertThat(body).isNotNull();

        List<Map> resourceSpans = (List<Map>) body.get("resourceSpans");
        assertThat(resourceSpans).isNotNull().isNotEmpty();

        List<Map> scopeSpans = (List<Map>) resourceSpans.get(0).get("scopeSpans");
        List<Map> spans = (List<Map>) scopeSpans.get(0).get("spans");
        assertThat(spans).isNotEmpty();

        String otelTraceId = (String) spans.get(0).get("traceId");
        // Should be 32-char hex (UUID without dashes)
        assertThat(otelTraceId).isNotNull();
        assertThat(otelTraceId).hasSize(32);
        assertThat(otelTraceId).matches("[0-9a-f]{32}");
        // Must match expected hex from source traceId
        assertThat(otelTraceId).isEqualTo(traceId.toString().replace("-", ""));
    }

    @Test
    void exportOtel_shouldMapSpanAttributesCorrectly() {
        UUID traceId = createTraceWithSpans();

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/" + traceId + "/otel",
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Map> resourceSpans = (List<Map>) response.getBody().get("resourceSpans");
        List<Map> scopeSpans = (List<Map>) resourceSpans.get(0).get("scopeSpans");
        List<Map> spans = (List<Map>) scopeSpans.get(0).get("spans");

        // Verify all spans have required fields
        for (Map span : spans) {
            assertThat(span.get("traceId")).isNotNull();
            assertThat(span.get("spanId")).isNotNull();
            assertThat(span.get("name")).isNotNull();
            assertThat(span.get("startTimeUnixNano")).isNotNull();
            assertThat(span.get("endTimeUnixNano")).isNotNull();
            assertThat(span.get("status")).isNotNull();

            List<Map> attrs = (List<Map>) span.get("attributes");
            assertThat(attrs).isNotNull().isNotEmpty();

            // Check for hookwatch.span.type attribute
            boolean hasSpanType = attrs.stream()
                    .anyMatch(a -> "hookwatch.span.type".equals(a.get("key")));
            assertThat(hasSpanType).as("Expected hookwatch.span.type attribute").isTrue();
        }
    }

    @Test
    void exportOtel_shouldMapSpanKindsCorrectly() {
        // Create trace with different span types
        String traceJson = """
                {
                    "agentId": "%s",
                    "status": "COMPLETED",
                    "totalTokens": 500,
                    "totalCost": 0.005,
                    "spans": [
                        {"name":"llm","type":"LLM_CALL","status":"COMPLETED","model":"gpt-4","sortOrder":0},
                        {"name":"tool","type":"TOOL_CALL","status":"COMPLETED","sortOrder":1},
                        {"name":"retrieval","type":"RETRIEVAL","status":"COMPLETED","sortOrder":2}
                    ]
                }
                """.formatted(agentId);

        HttpEntity<String> entity = new HttpEntity<>(traceJson, authHeaders(apiKey));
        ResponseEntity<Map> created = restTemplate.postForEntity(baseUrl() + "/traces", entity, Map.class);
        UUID traceId = UUID.fromString((String) created.getBody().get("id"));

        ResponseEntity<Map> otelResponse = restTemplate.exchange(
                baseUrl() + "/traces/" + traceId + "/otel",
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        List<Map> spans = getSpans(otelResponse.getBody());
        assertThat(spans).hasSize(3);

        Map llmSpan = spans.stream().filter(s -> "llm".equals(s.get("name"))).findFirst().orElseThrow();
        Map toolSpan = spans.stream().filter(s -> "tool".equals(s.get("name"))).findFirst().orElseThrow();
        Map retrievalSpan = spans.stream().filter(s -> "retrieval".equals(s.get("name"))).findFirst().orElseThrow();

        // LLM_CALL → INTERNAL (1)
        assertThat(((Number) llmSpan.get("kind")).intValue()).isEqualTo(1);
        // TOOL_CALL → CLIENT (3)
        assertThat(((Number) toolSpan.get("kind")).intValue()).isEqualTo(3);
        // RETRIEVAL → CLIENT (3)
        assertThat(((Number) retrievalSpan.get("kind")).intValue()).isEqualTo(3);
    }

    @Test
    void exportOtel_shouldReturn403ForCrossTenantAccess() {
        UUID traceId = createTraceWithSpans();

        // Create a second tenant
        String otherApiKey = createTenantAndGetKey("otel-other-tenant-" + UUID.randomUUID());

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/" + traceId + "/otel",
                HttpMethod.GET, new HttpEntity<>(authHeaders(otherApiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void exportOtel_shouldReturn404ForNonExistentTrace() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/" + UUID.randomUUID() + "/otel",
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── Ingest Tests ─────────────────────────────────────────────────────────

    @Test
    void ingestOtel_shouldCreateTraceWithCorrectSpans() {
        OtelPayload payload = new OtelPayload(agentId.toString(), tenantId);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/ingest/otel",
                new HttpEntity<>(payload.toJson(), authHeaders(apiKey)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("traceId")).isNotNull();
        assertThat(((Number) body.get("spanCount")).intValue()).isEqualTo(2);
        assertThat(body.get("status")).isEqualTo("COMPLETED");
    }

    @Test
    void ingestOtel_shouldBeRetrievableAfterIngestion() {
        OtelPayload payload = new OtelPayload(agentId.toString(), tenantId);

        ResponseEntity<Map> ingestResponse = restTemplate.postForEntity(
                baseUrl() + "/ingest/otel",
                new HttpEntity<>(payload.toJson(), authHeaders(apiKey)),
                Map.class);

        String traceId = (String) ingestResponse.getBody().get("traceId");

        // Verify trace is accessible via normal GET endpoint
        ResponseEntity<Map> getResponse = restTemplate.exchange(
                baseUrl() + "/traces/" + traceId,
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map> spans = (List<Map>) getResponse.getBody().get("spans");
        assertThat(spans).hasSize(2);
    }

    @Test
    void ingestOtel_roundTrip_exportedPayloadMatchesOriginal() {
        // Create original trace with specific data
        UUID originalTraceId = createTraceWithSpans();

        // Export it to OTLP
        ResponseEntity<Map> exportResponse = restTemplate.exchange(
                baseUrl() + "/traces/" + originalTraceId + "/otel",
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);
        assertThat(exportResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // Ingest the exported payload back
        ResponseEntity<Map> ingestResponse = restTemplate.postForEntity(
                baseUrl() + "/ingest/otel",
                new HttpEntity<>(exportResponse.getBody(), authHeaders(apiKey)),
                Map.class);

        assertThat(ingestResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String newTraceId = (String) ingestResponse.getBody().get("traceId");
        assertThat(newTraceId).isNotNull();
        assertThat(newTraceId).isNotEqualTo(originalTraceId.toString()); // New ID on ingest

        // Compare span count
        int originalSpanCount = getSpanCount(originalTraceId);
        int ingestedSpanCount = ((Number) ingestResponse.getBody().get("spanCount")).intValue();
        assertThat(ingestedSpanCount).isEqualTo(originalSpanCount);
    }

    @Test
    void ingestOtel_shouldReturn400WhenAgentIdMissing() {
        String payloadWithoutAgentId = """
                {"resourceSpans":[{"resource":{"attributes":[]},"scopeSpans":[]}]}
                """;

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/ingest/otel",
                new HttpEntity<>(payloadWithoutAgentId, authHeaders(apiKey)),
                Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID createTraceWithSpans() {
        String traceJson = """
                {
                    "agentId": "%s",
                    "status": "COMPLETED",
                    "totalTokens": 800,
                    "totalCost": 0.008,
                    "spans": [
                        {
                            "name": "llm-call",
                            "type": "LLM_CALL",
                            "status": "COMPLETED",
                            "inputTokens": 400,
                            "outputTokens": 200,
                            "cost": 0.005,
                            "model": "gpt-4",
                            "sortOrder": 0
                        },
                        {
                            "name": "tool-call",
                            "type": "TOOL_CALL",
                            "status": "COMPLETED",
                            "inputTokens": 100,
                            "outputTokens": 100,
                            "cost": 0.003,
                            "sortOrder": 1
                        }
                    ]
                }
                """.formatted(agentId);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces", new HttpEntity<>(traceJson, authHeaders(apiKey)), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    private int getSpanCount(UUID traceId) {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/" + traceId,
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);
        List<Map> spans = (List<Map>) response.getBody().get("spans");
        return spans == null ? 0 : spans.size();
    }

    private List<Map> getSpans(Map otelBody) {
        List<Map> resourceSpans = (List<Map>) otelBody.get("resourceSpans");
        List<Map> scopeSpans = (List<Map>) resourceSpans.get(0).get("scopeSpans");
        return (List<Map>) scopeSpans.get(0).get("spans");
    }

    record OtelPayload(String agentId, String tenantId) {
        String toJson() {
            return """
                    {
                      "resourceSpans": [{
                        "resource": {
                          "attributes": [
                            {"key":"hookwatch.agent.id","value":{"stringValue":"%s"}},
                            {"key":"hookwatch.tenant.id","value":{"stringValue":"%s"}},
                            {"key":"service.name","value":{"stringValue":"test-agent"}}
                          ]
                        },
                        "scopeSpans": [{
                          "scope": {"name":"hookwatch","version":"1.0.0"},
                          "spans": [
                            {
                              "traceId":"aabbccddeeff00112233445566778899",
                              "spanId":"aabbccddeeff0011",
                              "name":"llm-call",
                              "kind":1,
                              "startTimeUnixNano":"1711411200000000000",
                              "endTimeUnixNano":"1711411201000000000",
                              "status":{"code":1,"message":"OK"},
                              "attributes":[
                                {"key":"hookwatch.span.type","value":{"stringValue":"LLM_CALL"}},
                                {"key":"hookwatch.tokens.input","value":{"intValue":300}},
                                {"key":"hookwatch.tokens.output","value":{"intValue":150}},
                                {"key":"hookwatch.model","value":{"stringValue":"gpt-4"}}
                              ]
                            },
                            {
                              "traceId":"aabbccddeeff00112233445566778899",
                              "spanId":"aabbccddeeff0022",
                              "parentSpanId":"aabbccddeeff0011",
                              "name":"tool-call",
                              "kind":3,
                              "startTimeUnixNano":"1711411201000000000",
                              "endTimeUnixNano":"1711411202000000000",
                              "status":{"code":1,"message":"OK"},
                              "attributes":[
                                {"key":"hookwatch.span.type","value":{"stringValue":"TOOL_CALL"}},
                                {"key":"hookwatch.tokens.input","value":{"intValue":50}},
                                {"key":"hookwatch.tokens.output","value":{"intValue":50}}
                              ]
                            }
                          ]
                        }]
                      }]
                    }
                    """.formatted(agentId, tenantId);
        }
    }
}

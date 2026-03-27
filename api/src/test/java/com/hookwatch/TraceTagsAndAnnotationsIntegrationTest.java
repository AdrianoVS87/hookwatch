package com.hookwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TraceTagsAndAnnotationsIntegrationTest extends BaseIntegrationTest {

    private String apiKeyA;
    private String apiKeyB;
    private UUID agentA;
    private UUID traceA1;
    private UUID traceA2;

    @BeforeEach
    void setup() {
        String[] tenantA = createTenantAndGetKeyAndId("tags-tenant-a-" + UUID.randomUUID());
        apiKeyA = tenantA[0];
        agentA = createAgent(apiKeyA, tenantA[1], "agent-a");

        String[] tenantB = createTenantAndGetKeyAndId("tags-tenant-b-" + UUID.randomUUID());
        apiKeyB = tenantB[0];
        UUID agentB = createAgent(apiKeyB, tenantB[1], "agent-b");

        traceA1 = createTrace(apiKeyA, agentA, "COMPLETED", 100, "0.001");
        traceA2 = createTrace(apiKeyA, agentA, "FAILED", 50, "0.0005");
        UUID traceB = createTrace(apiKeyB, agentB, "COMPLETED", 10, "0.0001");

        setTags(apiKeyA, traceA1, "production", "reviewed");
        setTags(apiKeyA, traceA2, "staging");
        setTags(apiKeyB, traceB, "production", "other-tenant");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMergeAndDeleteTagsDeterministically() {
        ResponseEntity<Map> mergeResp = restTemplate.exchange(
                baseUrl() + "/traces/" + traceA1 + "/tags",
                HttpMethod.POST,
                new HttpEntity<>("{\"tags\":[\"Production\",\"critical\",\"reviewed\"]}", authHeaders(apiKeyA)),
                Map.class
        );

        assertThat(mergeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<String> mergedTags = (List<String>) mergeResp.getBody().get("tags");
        assertThat(mergedTags).containsExactly("critical", "production", "reviewed");

        ResponseEntity<Void> deleteResp = restTemplate.exchange(
                baseUrl() + "/traces/" + traceA1 + "/tags/reviewed",
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(apiKeyA)),
                Void.class
        );

        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map> traceResp = restTemplate.exchange(
                baseUrl() + "/traces/" + traceA1,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(apiKeyA)),
                Map.class
        );

        List<String> tags = (List<String>) traceResp.getBody().get("tags");
        assertThat(tags).containsExactly("critical", "production");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldFilterByTagAndListUniqueTagsForTenant() {
        ResponseEntity<Map> filteredResp = restTemplate.exchange(
                baseUrl() + "/traces?agentId=" + agentA + "&tag=production&page=0&size=20",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(apiKeyA)),
                Map.class
        );

        assertThat(filteredResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> content = (List<Map<String, Object>>) filteredResp.getBody().get("content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("id")).isEqualTo(traceA1.toString());

        ResponseEntity<List> tagsResp = restTemplate.exchange(
                baseUrl() + "/tags",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(apiKeyA)),
                List.class
        );

        assertThat(tagsResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tagsResp.getBody()).containsExactly("production", "reviewed", "staging");
        assertThat(tagsResp.getBody()).doesNotContain("other-tenant");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCreateAndListAnnotations() {
        ResponseEntity<Map> createResp = restTemplate.exchange(
                baseUrl() + "/traces/" + traceA1 + "/annotations",
                HttpMethod.POST,
                new HttpEntity<>("{\"text\":\"Need to inspect timeout behavior\",\"author\":\"adriano\"}", authHeaders(apiKeyA)),
                Map.class
        );

        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResp.getBody().get("author")).isEqualTo("adriano");
        assertThat(createResp.getBody().get("traceId")).isEqualTo(traceA1.toString());

        ResponseEntity<List> listResp = restTemplate.exchange(
                baseUrl() + "/traces/" + traceA1 + "/annotations",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(apiKeyA)),
                List.class
        );

        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResp.getBody()).hasSize(1);
        Map<String, Object> annotation = (Map<String, Object>) listResp.getBody().get(0);
        assertThat(annotation.get("text")).isEqualTo("Need to inspect timeout behavior");

        ResponseEntity<Map> forbiddenResp = restTemplate.exchange(
                baseUrl() + "/traces/" + traceA1 + "/annotations",
                HttpMethod.POST,
                new HttpEntity<>("{\"text\":\"x\",\"author\":\"tenant-b\"}", authHeaders(apiKeyB)),
                Map.class
        );

        assertThat(forbiddenResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private void setTags(String apiKey, UUID traceId, String... tags) {
        String payload = "{\"tags\":[\"" + String.join("\",\"", tags) + "\"]}";
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces/" + traceId + "/tags",
                HttpMethod.POST,
                new HttpEntity<>(payload, authHeaders(apiKey)),
                Map.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

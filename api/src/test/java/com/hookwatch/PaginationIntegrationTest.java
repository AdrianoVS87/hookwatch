package com.hookwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaginationIntegrationTest extends BaseIntegrationTest {

    private String apiKey;
    private UUID agentId;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        String[] keyAndId = createTenantAndGetKeyAndId("pagination-tenant-" + UUID.randomUUID());
        apiKey = keyAndId[0];
        String tenantId = keyAndId[1];
        agentId = createAgent(apiKey, tenantId, "pagination-agent");

        // Create 25 traces
        for (int i = 0; i < 25; i++) {
            String traceJson = """
                {
                    "agentId": "%s",
                    "status": "COMPLETED",
                    "totalTokens": %d,
                    "totalCost": 0.001,
                    "spans": []
                }
                """.formatted(agentId, 100 + i);
            HttpEntity<String> entity = new HttpEntity<>(traceJson, authHeaders(apiKey));
            restTemplate.postForEntity(baseUrl() + "/traces", entity, Map.class);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnFirstPageWith10Results() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces?agentId=" + agentId + "&page=0&size=10",
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = response.getBody();
        List content = (List) body.get("content");
        assertThat(content).hasSize(10);
        assertThat(((Number) body.get("totalElements")).intValue()).isEqualTo(25);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnLastPageWith5Results() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces?agentId=" + agentId + "&page=2&size=10",
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = response.getBody();
        List content = (List) body.get("content");
        assertThat(content).hasSize(5);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSortByStartedAtDescByDefault() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces?agentId=" + agentId + "&page=0&size=25&sort=startedAt,desc",
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKey)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(25);

        // Verify descending order by startedAt
        for (int i = 0; i < content.size() - 1; i++) {
            String current = (String) content.get(i).get("startedAt");
            String next = (String) content.get(i + 1).get("startedAt");
            assertThat(current.compareTo(next)).isGreaterThanOrEqualTo(0);
        }
    }
}

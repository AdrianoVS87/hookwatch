package com.hookwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantIsolationIntegrationTest extends BaseIntegrationTest {

    private String apiKeyA;
    private String tenantIdA;
    private String apiKeyB;
    private String tenantIdB;
    private UUID agentIdA;

    @BeforeEach
    void setUp() {
        String[] a = createTenantAndGetKeyAndId("tenant-A-" + UUID.randomUUID());
        apiKeyA = a[0];
        tenantIdA = a[1];

        String[] b = createTenantAndGetKeyAndId("tenant-B-" + UUID.randomUUID());
        apiKeyB = b[0];
        tenantIdB = b[1];

        agentIdA = createAgent(apiKeyA, tenantIdA, "agent-A");

        // Create a trace under tenant A
        String traceJson = """
            {
                "agentId": "%s",
                "status": "COMPLETED",
                "totalTokens": 100,
                "totalCost": 0.001,
                "spans": []
            }
            """.formatted(agentIdA);
        HttpEntity<String> entity = new HttpEntity<>(traceJson, authHeaders(apiKeyA));
        restTemplate.postForEntity(baseUrl() + "/traces", entity, Map.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void tenantBShouldNotSeeTenantAAgents() {
        ResponseEntity<List> response = restTemplate.exchange(
                baseUrl() + "/agents",
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKeyB)), List.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void tenantBShouldNotSeeTenantATraces() {
        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/traces?agentId=" + agentIdA + "&page=0&size=10",
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKeyB)), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map body = response.getBody();
        List content = (List) body.get("content");
        assertThat(content).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void tenantACanSeeOwnAgentsAndTraces() {
        // Verify tenant A sees its own agent
        ResponseEntity<List> agentsResp = restTemplate.exchange(
                baseUrl() + "/agents",
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKeyA)), List.class);
        assertThat(agentsResp.getBody()).hasSize(1);

        // Verify tenant A sees its own trace
        ResponseEntity<Map> tracesResp = restTemplate.exchange(
                baseUrl() + "/traces?agentId=" + agentIdA + "&page=0&size=10",
                HttpMethod.GET, new HttpEntity<>(authHeaders(apiKeyA)), Map.class);
        List content = (List) tracesResp.getBody().get("content");
        assertThat(content).hasSize(1);
    }
}

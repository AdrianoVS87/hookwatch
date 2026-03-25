package com.hookwatch;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    static final PostgreSQLContainer<?> postgres;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("hookwatch_test")
                .withUsername("test")
                .withPassword("test");
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    protected String baseUrl() {
        return "http://localhost:" + port + "/api/v1";
    }

    @SuppressWarnings("unchecked")
    protected String createTenantAndGetKey(String name) {
        String[] result = createTenantAndGetKeyAndId(name);
        return result[0];
    }

    @SuppressWarnings("unchecked")
    protected String[] createTenantAndGetKeyAndId(String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"name\":\"" + name + "\"}", headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/tenants", entity, Map.class);
        assertThat(response.getStatusCode()).as("Tenant creation failed: " + response.getBody()).isEqualTo(HttpStatus.CREATED);
        Map body = response.getBody();
        assertThat(body).isNotNull();
        return new String[]{(String) body.get("apiKey"), (String) body.get("id")};
    }

    @SuppressWarnings("unchecked")
    protected UUID createAgent(String apiKey, String tenantId, String name) {
        HttpHeaders headers = authHeaders(apiKey);
        String json = "{\"tenantId\":\"" + tenantId + "\",\"name\":\"" + name + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/agents", entity, Map.class);
        assertThat(response.getStatusCode()).as("Agent creation failed: " + response.getBody()).isEqualTo(HttpStatus.CREATED);
        Map body = response.getBody();
        assertThat(body).isNotNull();
        return UUID.fromString((String) body.get("id"));
    }

    @SuppressWarnings("unchecked")
    protected UUID createTrace(String apiKey, UUID agentId, String status, int tokens, String costStr) {
        String traceJson = """
            {
                "agentId": "%s",
                "status": "%s",
                "totalTokens": %d,
                "totalCost": %s,
                "spans": []
            }
            """.formatted(agentId, status, tokens, costStr);
        HttpEntity<String> entity = new HttpEntity<>(traceJson, authHeaders(apiKey));
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/traces", entity, Map.class);
        assertThat(response.getStatusCode()).as("Trace creation failed: " + response.getBody()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    protected HttpHeaders authHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}

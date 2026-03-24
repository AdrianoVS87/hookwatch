package com.hookwatch;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("hookwatch_test")
            .withUsername("test")
            .withPassword("test");

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

    /**
     * Creates a tenant via API and returns the raw API key from the response.
     */
    @SuppressWarnings("unchecked")
    protected String createTenantAndGetKey(String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"name\":\"" + name + "\"}", headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/tenants", entity, Map.class);
        return (String) response.getBody().get("apiKey");
    }

    /**
     * Creates a tenant and returns both the API key and tenant ID.
     */
    @SuppressWarnings("unchecked")
    protected String[] createTenantAndGetKeyAndId(String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>("{\"name\":\"" + name + "\"}", headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/tenants", entity, Map.class);
        Map body = response.getBody();
        return new String[]{(String) body.get("apiKey"), (String) body.get("id")};
    }

    /**
     * Creates an agent under the given tenant and returns the agent ID.
     */
    @SuppressWarnings("unchecked")
    protected UUID createAgent(String apiKey, String tenantId, String name) {
        HttpHeaders headers = authHeaders(apiKey);
        String json = "{\"tenantId\":\"" + tenantId + "\",\"name\":\"" + name + "\"}";
        HttpEntity<String> entity = new HttpEntity<>(json, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                baseUrl() + "/agents", entity, Map.class);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    protected HttpHeaders authHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}

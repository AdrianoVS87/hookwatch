package com.hookwatch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiKeyAuthIntegrationTest extends BaseIntegrationTest {

    private String validApiKey;

    @BeforeEach
    void setUp() {
        validApiKey = createTenantAndGetKey("auth-tenant-" + UUID.randomUUID());
    }

    @Test
    void validKeyShouldReturn200() {
        ResponseEntity<Object> response = restTemplate.exchange(
                baseUrl() + "/agents",
                HttpMethod.GET, new HttpEntity<>(authHeaders(validApiKey)), Object.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void invalidKeyShouldReturn403() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", "totally-invalid-key-" + UUID.randomUUID());
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/agents",
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void missingApiKeyHeaderShouldReturn401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // No X-API-Key header

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/agents",
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void emptyApiKeyShouldReturn401() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", "");
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                baseUrl() + "/agents",
                HttpMethod.GET, new HttpEntity<>(headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

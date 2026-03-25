package com.hookwatch.repository;

import com.hookwatch.BaseIntegrationTest;
import com.hookwatch.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void findByApiKey_returnsCorrectTenant() {
        String apiKey = UUID.randomUUID().toString();
        Tenant tenant = Tenant.builder()
                .name("test-tenant-" + UUID.randomUUID())
                .apiKey(apiKey)
                .build();
        tenantRepository.save(tenant);

        Optional<Tenant> found = tenantRepository.findByApiKey(apiKey);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo(tenant.getName());
    }

    @Test
    void findByApiKey_withInvalidKey_returnsEmpty() {
        Optional<Tenant> found = tenantRepository.findByApiKey("nonexistent-key");
        assertThat(found).isEmpty();
    }
}

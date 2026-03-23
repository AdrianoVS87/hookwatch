package com.hookwatch.repository;

import com.hookwatch.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("dev")
class TenantRepositoryTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void findByApiKey_returnsCorrectTenant() {
        String apiKey = UUID.randomUUID().toString();
        Tenant tenant = Tenant.builder()
                .name("test-tenant")
                .apiKey(apiKey)
                .build();
        tenantRepository.save(tenant);

        Optional<Tenant> found = tenantRepository.findByApiKey(apiKey);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("test-tenant");
    }

    @Test
    void findByApiKey_withInvalidKey_returnsEmpty() {
        Optional<Tenant> found = tenantRepository.findByApiKey("nonexistent-key");
        assertThat(found).isEmpty();
    }
}

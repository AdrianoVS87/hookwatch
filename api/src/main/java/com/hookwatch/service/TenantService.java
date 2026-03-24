package com.hookwatch.service;

import com.hookwatch.domain.Tenant;
import com.hookwatch.dto.TenantDto;
import com.hookwatch.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    /**
     * Creates a new tenant and generates a hashed API key.
     *
     * The raw API key is returned ONCE in the response and is unrecoverable after.
     * Format: "{tenantId}.{secret}" — tenant ID prefix enables efficient lookup in ApiKeyFilter.
     *
     * @return Tenant entity with apiKey field set to the RAW key (for one-time display).
     *         The stored value is a BCrypt hash. The returned object is not a DB entity.
     */
    @Transactional
    public Tenant create(TenantDto dto) {
        // First, persist the tenant with a temporary unique key to get the generated UUID
        String secret = UUID.randomUUID().toString().replace("-", "");
        String tempKey = "temp-" + UUID.randomUUID();
        Tenant tenant = Tenant.builder()
                .name(dto.getName())
                .apiKey(tempKey)
                .build();
        Tenant saved = tenantRepository.saveAndFlush(tenant);

        // Now build the API key using the generated tenant ID
        String rawKey = saved.getId() + "." + secret;
        String hashedKey = passwordEncoder.encode(rawKey);
        saved.setApiKey(hashedKey);
        tenantRepository.saveAndFlush(saved);

        entityManager.detach(saved);

        // Return a new detached object with the raw key for one-time display.
        return Tenant.builder()
                .id(saved.getId())
                .name(saved.getName())
                .apiKey(rawKey)
                .createdAt(saved.getCreatedAt())
                .build();
    }
}

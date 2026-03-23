package com.hookwatch.service;

import com.hookwatch.domain.Tenant;
import com.hookwatch.dto.TenantDto;
import com.hookwatch.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a new tenant and generates a hashed API key.
     *
     * The raw API key is returned ONCE in the response and is unrecoverable after.
     * Format: "{tenantId}.{secret}" — tenant ID prefix enables efficient lookup in ApiKeyFilter.
     *
     * @return Tenant entity with apiKey field set to the RAW key (for one-time display).
     *         The stored value is a BCrypt hash. The returned object is not a DB entity.
     */
    public Tenant create(TenantDto dto) {
        UUID tenantId = UUID.randomUUID();
        String secret = UUID.randomUUID().toString().replace("-", "");
        String rawKey = tenantId + "." + secret;
        String hashedKey = passwordEncoder.encode(rawKey);

        Tenant tenant = Tenant.builder()
                .id(tenantId)
                .name(dto.getName())
                .apiKey(hashedKey)
                .build();
        Tenant saved = tenantRepository.save(tenant);

        // Return entity with raw key set — this is the only time it's visible.
        // The caller (TenantController) returns this directly; the stored hash is in the DB.
        saved.setApiKey(rawKey);
        return saved;
    }
}

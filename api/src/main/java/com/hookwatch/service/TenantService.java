package com.hookwatch.service;

import com.hookwatch.domain.Tenant;
import com.hookwatch.dto.TenantDto;
import com.hookwatch.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    public Tenant create(TenantDto dto) {
        Tenant tenant = Tenant.builder()
                .name(dto.getName())
                .apiKey(UUID.randomUUID().toString())
                .build();
        return tenantRepository.save(tenant);
    }
}

package com.hookwatch.filter;

import com.hookwatch.domain.Tenant;
import com.hookwatch.repository.TenantRepository;
import com.hookwatch.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String normalizedPath = path == null ? "" : path.replaceAll("/+$", "");

        // Skip auth for Swagger UI, API docs, actuator, health check, and tenant bootstrap endpoint.
        // Use normalized path + startsWith for docs endpoints to be robust across proxies/trailing slashes.
        if (normalizedPath.startsWith("/swagger-ui") || normalizedPath.startsWith("/v3/api-docs")
                || normalizedPath.startsWith("/api/v1/openapi.json") || normalizedPath.startsWith("/actuator")
                || normalizedPath.equals("/api/v1/tenants") || normalizedPath.equals("/api/v1/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Only accept X-API-Key header — no query param fallback (prevents log leakage)
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing X-API-Key header\"}");
            return;
        }

        Optional<Tenant> tenantOpt = findTenantByApiKey(apiKey);
        if (tenantOpt.isEmpty()) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write("{\"error\":\"Invalid API key\"}");
            return;
        }

        Tenant tenant = tenantOpt.get();
        request.setAttribute("authenticatedTenantId", tenant.getId());
        request.setAttribute("authenticatedTenant", tenant);

        try {
            TenantContext.set(tenant.getId());
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Finds a tenant whose stored BCrypt hash matches the provided raw API key.
     * API keys use the format "{tenantId}.{secret}" to enable efficient lookup
     * without iterating all tenants.
     *
     * Legacy keys (not containing ".") fall back to scanning all tenants —
     * safe for migration period (few tenants in demo). After migration,
     * all keys use the new format.
     */
    private Optional<Tenant> findTenantByApiKey(String rawApiKey) {
        int dotIndex = rawApiKey.indexOf('.');
        if (dotIndex > 0) {
            // New format: "{tenantId}.{secret}" — look up directly by tenant id prefix
            String tenantIdStr = rawApiKey.substring(0, dotIndex);
            try {
                java.util.UUID tenantId = java.util.UUID.fromString(tenantIdStr);
                return tenantRepository.findById(tenantId)
                        .filter(t -> passwordEncoder.matches(rawApiKey, t.getApiKey()));
            } catch (IllegalArgumentException e) {
                // Not a valid UUID prefix — fall through to full scan
            }
        }

        // Legacy format: scan all tenants (only used during migration period)
        return tenantRepository.findAll().stream()
                .filter(t -> {
                    try {
                        return passwordEncoder.matches(rawApiKey, t.getApiKey());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .findFirst();
    }
}

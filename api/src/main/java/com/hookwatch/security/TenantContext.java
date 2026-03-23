package com.hookwatch.security;

import java.util.UUID;

/**
 * Thread-local tenant context — set by ApiKeyFilter, cleared after request.
 * Avoids passing tenantId through every method signature.
 * Uses try/finally in the filter to guarantee cleanup even on exceptions.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(UUID tenantId) {
        CURRENT.set(tenantId);
    }

    public static UUID get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}

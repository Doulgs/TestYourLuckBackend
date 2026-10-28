package com.doulgs.testyourluck.shared.tenant;

import java.util.Optional;
import java.util.UUID;

/**
 * Tenant da requisição corrente. Preenchido pelo filtro de autenticação a partir do JWT
 * (ou explicitamente no fluxo de cadastro) e sempre limpo ao final da requisição.
 */
public final class TenantContext {

    public record Tenant(UUID tenant, UUID owner) {
    }

    private static final ThreadLocal<Tenant> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(UUID tenant, UUID owner) {
        CURRENT.set(new Tenant(tenant, owner));
    }

    public static Optional<Tenant> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static Tenant require() {
        return current().orElseThrow(() -> new IllegalStateException("Nenhum tenant no contexto da requisição"));
    }

    public static void clear() {
        CURRENT.remove();
    }
}

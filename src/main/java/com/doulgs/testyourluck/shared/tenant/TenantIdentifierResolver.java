package com.doulgs.testyourluck.shared.tenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

import java.util.UUID;

/**
 * Informa ao Hibernate o tenant corrente. Sem tenant no contexto (login/cadastro) devolve o tenant
 * "root", que desliga o filtro automático de {@code @TenantId}.
 */
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<UUID> {

    public static final UUID ROOT = new UUID(0L, 0L);

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContext.current().map(TenantContext.Tenant::tenant).orElse(ROOT);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public boolean isRoot(UUID tenantId) {
        return ROOT.equals(tenantId);
    }
}

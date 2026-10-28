package com.doulgs.testyourluck.dashboard;

import com.doulgs.testyourluck.dashboard.DashboardDtos.DashboardResponse;
import com.doulgs.testyourluck.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Cache dos agregados do dashboard por tenant e período (TTL 60s), com fallback quando o Redis falha. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardCache {

    static final Duration TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;
    private final JsonMapper json;

    public DashboardResponse get(UUID tenant, int range, Supplier<DashboardResponse> loader) {
        String key = key(tenant, range);
        try {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                return json.readValue(cached, DashboardResponse.class);
            }
        } catch (DataAccessException ex) {
            log.warn("Cache do dashboard indisponível: {}", ex.getMessage());
            return loader.get();
        }
        DashboardResponse fresh = loader.get();
        try {
            redis.opsForValue().set(key, json.writeValueAsString(fresh), TTL);
        } catch (DataAccessException ex) {
            log.warn("Falha ao gravar cache do dashboard: {}", ex.getMessage());
        }
        return fresh;
    }

    /** Invalida os agregados do tenant corrente; dentro de transação, somente após o commit. */
    public void evictCurrentTenant() {
        UUID tenant = TenantContext.require().tenant();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evict(tenant);
                }
            });
        } else {
            evict(tenant);
        }
    }

    void evict(UUID tenant) {
        try {
            redis.delete(List.of(key(tenant, 7), key(tenant, 30), key(tenant, 90)));
        } catch (DataAccessException ex) {
            log.warn("Falha ao invalidar cache do dashboard: {}", ex.getMessage());
        }
    }

    private static String key(UUID tenant, int range) {
        return "dashboard:" + tenant + ":" + range;
    }
}

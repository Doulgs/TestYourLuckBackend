package com.doulgs.testyourluck.draw;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Lock distribuído simples (SET NX PX + liberação compare-and-delete). Se o Redis estiver indisponível
 * a ação roda sem lock: a consistência final é garantida pela baixa condicional no banco.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLock {

    private static final Duration LEASE = Duration.ofSeconds(5);
    private static final Duration WAIT = Duration.ofSeconds(3);
    private static final DefaultRedisScript<Long> RELEASE = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate redis;

    public <T> T withLock(String key, Supplier<T> action) {
        String token = UUID.randomUUID().toString();
        boolean acquired;
        try {
            acquired = acquire(key, token);
        } catch (DataAccessException ex) {
            log.warn("Redis indisponível, executando sem lock distribuído: {}", ex.getMessage());
            return action.get();
        }
        if (!acquired) {
            log.warn("Timeout aguardando lock {}, seguindo com a baixa condicional", key);
            return action.get();
        }
        try {
            return action.get();
        } finally {
            try {
                redis.execute(RELEASE, List.of(key), token);
            } catch (DataAccessException ex) {
                log.warn("Falha ao liberar lock {}: {}", key, ex.getMessage());
            }
        }
    }

    private boolean acquire(String key, String token) {
        long deadline = System.nanoTime() + WAIT.toNanos();
        do {
            if (Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, token, LEASE))) {
                return true;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (System.nanoTime() < deadline);
        return false;
    }
}

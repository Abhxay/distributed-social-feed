package com.showcase.feed.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * No live Redis/Aiven required: StringRedisTemplate is mocked, delegating to a thread-safe
 * ConcurrentHashMap that faithfully simulates SETNX/GET/SET/DEL semantics. This still genuinely
 * exercises the race condition the service is designed to close.
 */
class IdempotencyServiceTest {

    private FakeRedisStore store;
    private IdempotencyService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        store = new FakeRedisStore();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);

        when(valueOps.setIfAbsent(any(), any(), any(Duration.class)))
            .thenAnswer(inv -> store.setIfAbsent(inv.getArgument(0), inv.getArgument(1)));
        when(valueOps.get(any()))
            .thenAnswer(inv -> store.get(inv.getArgument(0)));
        doAnswer(inv -> {
            store.set(inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(valueOps).set(any(), any(), any(Duration.class));
        when(redis.delete(anyString()))
            .thenAnswer(inv -> {
                store.delete(inv.getArgument(0));
                return true;
            });

        service = new IdempotencyService(redis, new ObjectMapper());
    }

    @Test
    void concurrentRequestsWithSameKeyExecuteActionOnlyOnce() throws Exception {
        AtomicInteger executions = new AtomicInteger();
        String key = UUID.randomUUID().toString();
        Callable<String> task = () -> service.execute(key, Map.of("x", 1), String.class,
            () -> {
                executions.incrementAndGet();
                return "result";
            });

        ExecutorService pool = Executors.newFixedThreadPool(5);
        List<Future<String>> futures = IntStream.range(0, 5)
            .mapToObj(i -> pool.submit(task)).toList();
        for (Future<String> f : futures) {
            assertEquals("result", f.get(5, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertEquals(1, executions.get());
    }

    @Test
    void sameKeyDifferentBodyThrowsConflict() {
        String key = UUID.randomUUID().toString();
        service.execute(key, Map.of("x", 1), String.class, () -> "ok");

        assertThrows(IdempotencyConflictException.class, () ->
            service.execute(key, Map.of("x", 2), String.class, () -> "ok"));
    }

    @Test
    void completedResponseIsReplayedOnRetryWithSameBody() {
        String key = UUID.randomUUID().toString();
        String first = service.execute(key, Map.of("x", 1), String.class, () -> "first-result");
        String second = service.execute(key, Map.of("x", 1), String.class, () -> "should-not-run");

        assertEquals("first-result", second);
    }

    static class FakeRedisStore {
        private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

        boolean setIfAbsent(String key, String value) {
            return map.putIfAbsent(key, value) == null;
        }

        String get(String key) {
            return map.get(key);
        }

        void set(String key, String value) {
            map.put(key, value);
        }

        void delete(String key) {
            map.remove(key);
        }
    }
}

package com.showcase.feed.common.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showcase.feed.common.util.JitteredBackoff;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.Supplier;

/**
 * Atomic idempotency-key store backed by Redis/Valkey. State machine per key:
 * ABSENT -> PROCESSING (SETNX reservation) -> COMPLETED (cached response).
 *
 * The PROCESSING lease is 10 minutes, far longer than any realistic execution time for the
 * actions this wraps (a handful of DB/Redis calls, sub-second). If an action ever ran longer
 * than the lease, a second caller could acquire a fresh reservation and re-execute it — this is
 * a known, accepted limitation given the current action set, not something this design solves.
 */
@Service
public class IdempotencyService {
    private static final Duration PROCESSING_TTL = Duration.ofMinutes(10);
    private static final Duration COMPLETED_TTL = Duration.ofMinutes(10);
    private static final int MAX_WAIT_ATTEMPTS = 3;

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public IdempotencyService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public <T> T execute(String idempotencyKey, Object requestBody, Class<T> responseType, Supplier<T> action) {
        String redisKey = "idem:" + idempotencyKey;
        String bodyHash = sha256(requestBody);

        for (int attempt = 0; attempt < MAX_WAIT_ATTEMPTS + 1; attempt++) {
            Boolean reserved = redis.opsForValue().setIfAbsent(
                redisKey, write(new IdemRecord(IdemState.PROCESSING, bodyHash, null)), PROCESSING_TTL);

            if (Boolean.TRUE.equals(reserved)) {
                try {
                    T result = action.get();
                    redis.opsForValue().set(
                        redisKey, write(new IdemRecord(IdemState.COMPLETED, bodyHash, result)), COMPLETED_TTL);
                    return result;
                } catch (RuntimeException e) {
                    // release the reservation so a legitimate retry isn't stuck for the full lease
                    redis.delete(redisKey);
                    throw e;
                }
            }

            String existingRaw = redis.opsForValue().get(redisKey);
            if (existingRaw == null) {
                continue; // reservation vanished between our check and now; loop and try to acquire
            }
            IdemRecord existing = read(existingRaw);
            if (!existing.bodyHash().equals(bodyHash)) {
                throw new IdempotencyConflictException("Idempotency key reused with a different request body");
            }
            if (existing.state() == IdemState.COMPLETED) {
                return objectMapper.convertValue(existing.response(), responseType);
            }
            if (attempt < MAX_WAIT_ATTEMPTS) {
                JitteredBackoff.sleep(attempt);
            }
        }
        throw new IdempotencyConflictException("Request with this idempotency key is still processing; retry shortly");
    }

    private String sha256(Object body) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(objectMapper.writeValueAsBytes(body));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash idempotency request body", e);
        }
    }

    private String write(IdemRecord record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private IdemRecord read(String raw) {
        try {
            return objectMapper.readValue(raw, IdemRecord.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}

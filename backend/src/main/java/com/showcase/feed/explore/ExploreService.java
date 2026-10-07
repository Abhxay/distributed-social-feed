package com.showcase.feed.explore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.showcase.feed.auth.User;
import com.showcase.feed.auth.UserRepository;
import com.showcase.feed.follows.FollowRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ExploreService {
    // Shared across every viewer (the ranked batch itself doesn't depend on who's asking —
    // only isFollowing does, and that's never cached, see getRanking()). Short TTL because
    // activity scores move in real time and this app's own copy on the page says "ranked by
    // weighted activity" - a long-stale batch would make that claim visibly false.
    private static final Duration BATCH_CACHE_TTL = Duration.ofSeconds(60);

    private final ExploreRanking exploreRanking;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public ExploreService(ExploreRanking exploreRanking, UserRepository userRepository,
                           FollowRepository followRepository, StringRedisTemplate redis,
                           ObjectMapper objectMapper) {
        this.exploreRanking = exploreRanking;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public List<ExploreUserResponse> getRanking(UUID currentUserId, int offset, int limit) {
        List<BatchEntry> batch = readBatchCache(offset, limit);
        if (batch == null) {
            batch = rebuildBatch(offset, limit);
            writeBatchCache(offset, limit, batch);
        }
        if (batch.isEmpty()) {
            return List.of();
        }

        // isFollowing is per-viewer and changes the moment someone clicks Follow/Unfollow on this
        // very page, so unlike the rest of the batch it's always computed live, cache or no cache.
        List<UUID> candidateIds = batch.stream().map(BatchEntry::userId).toList();
        Set<UUID> alreadyFollowing = followRepository.findFollowedIds(currentUserId, candidateIds);

        return batch.stream()
            .map(entry -> new ExploreUserResponse(
                entry.userId(), entry.username(), entry.activityScore(), alreadyFollowing.contains(entry.userId())))
            .toList();
    }

    public long totalRanked() {
        return exploreRanking.totalRanked();
    }

    private List<BatchEntry> rebuildBatch(int offset, int limit) {
        List<ExploreRanking.RankedUser> ranked = exploreRanking.topUsers(offset, limit);
        if (ranked.isEmpty()) {
            return List.of();
        }

        List<UUID> candidateIds = ranked.stream().map(ExploreRanking.RankedUser::userId).toList();
        Map<UUID, String> usernameById = new HashMap<>();
        for (User user : userRepository.findAllById(candidateIds)) {
            usernameById.put(user.getId(), user.getUsername());
        }

        return ranked.stream()
            .filter(entry -> usernameById.containsKey(entry.userId())) // skip any deleted/orphaned ranking entries
            .map(entry -> new BatchEntry(entry.userId(), usernameById.get(entry.userId()), entry.activityScore()))
            .toList();
    }

    private List<BatchEntry> readBatchCache(int offset, int limit) {
        String json = redis.opsForValue().get(batchCacheKey(offset, limit));
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<BatchEntry>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupt explore batch cache entry", e);
        }
    }

    private void writeBatchCache(int offset, int limit, List<BatchEntry> batch) {
        try {
            redis.opsForValue().set(batchCacheKey(offset, limit), objectMapper.writeValueAsString(batch), BATCH_CACHE_TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize explore batch cache entry", e);
        }
    }

    private String batchCacheKey(int offset, int limit) {
        return "explore:batch:" + offset + ":" + limit;
    }

    private record BatchEntry(UUID userId, String username, double activityScore) {}
}

package com.showcase.feed.explore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showcase.feed.auth.User;
import com.showcase.feed.auth.UserRepository;
import com.showcase.feed.follows.FollowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ExploreServiceTest {

    private ExploreRanking exploreRanking;
    private UserRepository userRepository;
    private FollowRepository followRepository;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;
    private ExploreService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        exploreRanking = mock(ExploreRanking.class);
        userRepository = mock(UserRepository.class);
        followRepository = mock(FollowRepository.class);
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valueOps);
        service = new ExploreService(exploreRanking, userRepository, followRepository, redis, new ObjectMapper());
    }

    @Test
    void cacheMissHydratesUsernamesAndMarksAlreadyFollowedUsersThenWritesCache() throws Exception {
        UUID viewer = UUID.randomUUID();
        UUID followedUser = UUID.randomUUID();
        UUID notFollowedUser = UUID.randomUUID();

        when(valueOps.get("explore:batch:0:50")).thenReturn(null);
        when(exploreRanking.topUsers(0, 50)).thenReturn(List.of(
            new ExploreRanking.RankedUser(followedUser, 9.0),
            new ExploreRanking.RankedUser(notFollowedUser, 3.0)));
        when(userRepository.findAllById(List.of(followedUser, notFollowedUser)))
            .thenReturn(List.of(userWithId(followedUser, "alice"), userWithId(notFollowedUser, "bob")));
        when(followRepository.findFollowedIds(eq(viewer), any())).thenReturn(Set.of(followedUser));

        List<ExploreUserResponse> result = service.getRanking(viewer, 0, 50);

        assertEquals(2, result.size());
        assertEquals("alice", result.get(0).username());
        assertTrue(result.get(0).isFollowing());
        assertEquals("bob", result.get(1).username());
        assertFalse(result.get(1).isFollowing());
        verify(valueOps).set(eq("explore:batch:0:50"), anyString(), any(Duration.class));
    }

    @Test
    void cacheHitSkipsRankingAndUsernameLookupButStillComputesFollowStateLive() {
        UUID viewer = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String cachedJson = "[{\"userId\":\"" + userId + "\",\"username\":\"cached_alice\",\"activityScore\":12.0}]";

        when(valueOps.get("explore:batch:0:50")).thenReturn(cachedJson);
        when(followRepository.findFollowedIds(eq(viewer), any())).thenReturn(Set.of());

        List<ExploreUserResponse> result = service.getRanking(viewer, 0, 50);

        assertEquals(1, result.size());
        assertEquals("cached_alice", result.get(0).username());
        assertFalse(result.get(0).isFollowing());
        verifyNoInteractions(exploreRanking, userRepository);
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void returnsEmptyListWithoutQueryingAnythingWhenRankingIsEmpty() {
        when(valueOps.get("explore:batch:0:50")).thenReturn(null);
        when(exploreRanking.topUsers(0, 50)).thenReturn(List.of());

        List<ExploreUserResponse> result = service.getRanking(UUID.randomUUID(), 0, 50);

        assertTrue(result.isEmpty());
        verifyNoInteractions(followRepository);
    }

    /** User's id is JPA-generated (no public setter), so set it via reflection for this test fixture. */
    private User userWithId(UUID id, String username) throws Exception {
        User user = new User(username, "hash");
        Field idField = User.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, id);
        return user;
    }
}

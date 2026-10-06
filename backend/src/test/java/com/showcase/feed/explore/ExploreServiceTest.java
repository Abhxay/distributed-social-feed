package com.showcase.feed.explore;

import com.showcase.feed.auth.User;
import com.showcase.feed.auth.UserRepository;
import com.showcase.feed.follows.FollowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExploreServiceTest {

    private ExploreRanking exploreRanking;
    private UserRepository userRepository;
    private FollowRepository followRepository;
    private ExploreService service;

    @BeforeEach
    void setUp() {
        exploreRanking = mock(ExploreRanking.class);
        userRepository = mock(UserRepository.class);
        followRepository = mock(FollowRepository.class);
        service = new ExploreService(exploreRanking, userRepository, followRepository);
    }

    @Test
    void hydratesUsernamesAndMarksAlreadyFollowedUsers() throws Exception {
        UUID viewer = UUID.randomUUID();
        UUID followedUser = UUID.randomUUID();
        UUID notFollowedUser = UUID.randomUUID();

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
    }

    @Test
    void returnsEmptyListWithoutQueryingAnythingWhenRankingIsEmpty() {
        when(exploreRanking.topUsers(0, 50)).thenReturn(List.of());

        List<ExploreUserResponse> result = service.getRanking(UUID.randomUUID(), 0, 50);

        assertTrue(result.isEmpty());
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

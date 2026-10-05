package com.showcase.feed.follows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FollowServiceTest {

    private FollowRepository followRepository;
    private FollowService service;

    @BeforeEach
    void setUp() {
        followRepository = mock(FollowRepository.class);
        service = new FollowService(followRepository);
    }

    @Test
    void followingYourselfThrowsWithoutTouchingTheRepository() {
        UUID userId = UUID.randomUUID();

        assertThrows(SelfFollowException.class, () -> service.follow(userId, userId));

        verify(followRepository, never()).save(any());
    }

    @Test
    void unfollowingSomethingNotFollowedDoesNotThrow() {
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        when(followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId)).thenReturn(0L);

        assertDoesNotThrow(() -> service.unfollow(followerId, followeeId));
    }
}

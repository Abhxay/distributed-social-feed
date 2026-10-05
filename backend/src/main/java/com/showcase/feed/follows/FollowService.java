package com.showcase.feed.follows;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FollowService {
    private final FollowRepository followRepository;

    public FollowService(FollowRepository followRepository) {
        this.followRepository = followRepository;
    }

    @Transactional
    public void follow(UUID followerId, UUID followeeId) {
        if (followerId.equals(followeeId)) {
            throw new SelfFollowException("cannot follow yourself");
        }
        try {
            Follow follow = new Follow();
            follow.setFollowerId(followerId);
            follow.setFolloweeId(followeeId);
            followRepository.save(follow);
        } catch (DataIntegrityViolationException e) {
            // duplicate follow attempt (composite PK already exists) — harmless no-op
        }
    }

    /** Idempotent DELETE: never throws, whether or not an edge existed to remove. */
    @Transactional
    public void unfollow(UUID followerId, UUID followeeId) {
        followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId);
    }
}

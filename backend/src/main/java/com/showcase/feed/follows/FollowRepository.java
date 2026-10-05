package com.showcase.feed.follows;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

    /** Used by FeedFanoutConsumer to fan a new post out to every follower's cached feed. */
    @Query("SELECT f.followerId FROM Follow f WHERE f.followeeId = :followeeId")
    List<UUID> findFollowerIds(@Param("followeeId") UUID followeeId);

    boolean existsByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);

    /** Derived delete query — Spring Data runs this transactionally and returns the row count removed. */
    long deleteByFollowerIdAndFolloweeId(UUID followerId, UUID followeeId);
}

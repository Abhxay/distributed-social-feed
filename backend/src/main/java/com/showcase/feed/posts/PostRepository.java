package com.showcase.feed.posts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    long countByAuthorId(UUID authorId);

    List<Post> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    @Query("SELECT COALESCE(SUM(p.likeCount), 0) FROM Post p WHERE p.authorId = :authorId")
    long sumLikesForAuthor(@Param("authorId") UUID authorId);

    /** Feed rebuild: posts authored by anyone the given user follows, newest first, backed by idx_posts_author_created. */
    @Query(value = "SELECT p.* FROM posts p JOIN follows f ON f.followee_id = p.author_id "
        + "WHERE f.follower_id = :userId ORDER BY p.created_at DESC LIMIT :limit", nativeQuery = true)
    List<Post> findFeedPosts(@Param("userId") UUID userId, @Param("limit") int limit);

    /** Same query, ids only — used by the cache lease's bounded fallback so it doesn't hydrate full entities. */
    @Query(value = "SELECT p.id FROM posts p JOIN follows f ON f.followee_id = p.author_id "
        + "WHERE f.follower_id = :userId ORDER BY p.created_at DESC LIMIT :limit", nativeQuery = true)
    List<UUID> findFeedPostIds(@Param("userId") UUID userId, @Param("limit") int limit);

    /**
     * Feed discovery: posts from anyone (not just people followed), eligible by being either
     * recent or well-liked. Merged into the feed at rebuild time so discovery stays a read-time
     * concern, not a second fan-out-on-write path.
     */
    @Query(value = "SELECT p.* FROM posts p WHERE p.author_id <> :userId "
        + "AND (p.created_at > :recentSince OR p.like_count >= :minLikes) "
        + "ORDER BY p.created_at DESC LIMIT :limit", nativeQuery = true)
    List<Post> findDiscoveryPosts(@Param("userId") UUID userId, @Param("recentSince") Instant recentSince,
                                   @Param("minLikes") int minLikes, @Param("limit") int limit);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = :count WHERE p.id = :postId")
    void updateLikeCount(@Param("postId") UUID postId, @Param("count") long count);

    @Modifying
    @Query("UPDATE Post p SET p.repostCount = :count WHERE p.id = :postId")
    void updateRepostCount(@Param("postId") UUID postId, @Param("count") long count);

    /**
     * One row per author: [authorId, postCount, totalLikesReceived]. Used only by
     * ExploreRankingBackfill to rebuild the Redis ranking from Postgres when it's empty
     * (fresh deploy, or Valkey data lost) — never on the normal request path.
     */
    @Query("SELECT p.authorId, COUNT(p), COALESCE(SUM(p.likeCount), 0) FROM Post p GROUP BY p.authorId")
    List<Object[]> aggregateActivityByAuthor();
}

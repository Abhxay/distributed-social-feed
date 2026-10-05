package com.showcase.feed.posts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {

    long countByAuthorId(UUID authorId);

    /** Feed rebuild: posts authored by anyone the given user follows, newest first, backed by idx_posts_author_created. */
    @Query(value = "SELECT p.* FROM posts p JOIN follows f ON f.followee_id = p.author_id "
        + "WHERE f.follower_id = :userId ORDER BY p.created_at DESC LIMIT :limit", nativeQuery = true)
    List<Post> findFeedPosts(@Param("userId") UUID userId, @Param("limit") int limit);

    /** Same query, ids only — used by the cache lease's bounded fallback so it doesn't hydrate full entities. */
    @Query(value = "SELECT p.id FROM posts p JOIN follows f ON f.followee_id = p.author_id "
        + "WHERE f.follower_id = :userId ORDER BY p.created_at DESC LIMIT :limit", nativeQuery = true)
    List<UUID> findFeedPostIds(@Param("userId") UUID userId, @Param("limit") int limit);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = :count WHERE p.id = :postId")
    void updateLikeCount(@Param("postId") UUID postId, @Param("count") long count);
}

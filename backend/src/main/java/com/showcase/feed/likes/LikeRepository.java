package com.showcase.feed.likes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, LikeId> {

    long countByPostId(UUID postId);

    @Modifying
    @Query(value = "INSERT INTO likes(user_id, post_id) VALUES (:userId, :postId) ON CONFLICT DO NOTHING",
        nativeQuery = true)
    int insertIgnoringConflict(@Param("userId") UUID userId, @Param("postId") UUID postId);

    @Modifying
    @Query(value = "DELETE FROM likes WHERE user_id = :userId AND post_id = :postId", nativeQuery = true)
    int deleteIfExists(@Param("userId") UUID userId, @Param("postId") UUID postId);

    @Query("SELECT l.postId FROM Like l WHERE l.userId = :userId AND l.postId IN :postIds")
    Set<UUID> findLikedPostIds(@Param("userId") UUID userId, @Param("postIds") List<UUID> postIds);
}

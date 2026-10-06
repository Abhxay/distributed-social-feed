package com.showcase.feed.reposts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RepostRepository extends JpaRepository<Repost, RepostId> {

    long countByPostId(UUID postId);

    List<Repost> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query(value = "INSERT INTO reposts(user_id, post_id) VALUES (:userId, :postId) ON CONFLICT DO NOTHING",
        nativeQuery = true)
    int insertIgnoringConflict(@Param("userId") UUID userId, @Param("postId") UUID postId);

    @Modifying
    @Query(value = "DELETE FROM reposts WHERE user_id = :userId AND post_id = :postId", nativeQuery = true)
    int deleteIfExists(@Param("userId") UUID userId, @Param("postId") UUID postId);

    @Query("SELECT r.postId FROM Repost r WHERE r.userId = :userId AND r.postId IN :postIds")
    Set<UUID> findRepostedPostIds(@Param("userId") UUID userId, @Param("postIds") List<UUID> postIds);
}

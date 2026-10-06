package com.showcase.feed.comments;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByPostIdOrderByCreatedAtAsc(UUID postId);

    /**
     * One row per post author: [authorId, commentsReceived]. Used only by
     * ExploreRankingBackfill — joins to posts natively since Comment has no JPA
     * relation to Post, just a plain postId column (same style as Like/Follow).
     */
    @Query(value = "SELECT p.author_id, COUNT(c.id) FROM comments c "
        + "JOIN posts p ON p.id = c.post_id GROUP BY p.author_id", nativeQuery = true)
    List<Object[]> aggregateCommentsReceivedByAuthor();
}

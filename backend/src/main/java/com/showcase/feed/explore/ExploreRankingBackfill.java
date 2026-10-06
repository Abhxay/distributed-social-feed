package com.showcase.feed.explore;

import com.showcase.feed.comments.CommentRepository;
import com.showcase.feed.posts.PostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Self-healing rebuild: if the Redis ranking is empty on startup (fresh deploy onto a database
 * that already has posts/likes, or Valkey data lost), recompute it once from Postgres — the
 * source of truth for posts and like counts — instead of starting with a permanently blank
 * Explore page. Once seeded, live activity keeps it current via ExploreRanking.creditPost/
 * creditLike in the normal write paths; this only ever runs when the ranking starts from zero.
 */
@Component
public class ExploreRankingBackfill implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ExploreRankingBackfill.class);

    private final ExploreRanking exploreRanking;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    public ExploreRankingBackfill(ExploreRanking exploreRanking, PostRepository postRepository,
                                   CommentRepository commentRepository) {
        this.exploreRanking = exploreRanking;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!exploreRanking.isEmpty()) {
            return;
        }

        Map<UUID, Long> commentsByAuthor = new HashMap<>();
        for (Object[] row : commentRepository.aggregateCommentsReceivedByAuthor()) {
            commentsByAuthor.put((UUID) row[0], ((Number) row[1]).longValue());
        }

        int seeded = 0;
        for (Object[] row : postRepository.aggregateActivityByAuthor()) {
            UUID authorId = (UUID) row[0];
            long postCount = (Long) row[1];
            long likesReceived = (Long) row[2];
            long commentsReceived = commentsByAuthor.getOrDefault(authorId, 0L);
            exploreRanking.seed(authorId, ExploreRanking.weighScore(postCount, likesReceived, commentsReceived));
            seeded++;
        }
        if (seeded > 0) {
            log.info("Backfilled explore ranking for {} authors from Postgres", seeded);
        }
    }
}

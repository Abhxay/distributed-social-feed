package com.showcase.feed.posts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showcase.feed.explore.ExploreRanking;
import com.showcase.feed.outbox.OutboxEvent;
import com.showcase.feed.outbox.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * No live Postgres/Kafka needed: both repositories are mocked. This proves the one thing
 * createPost exists for — the post row and its outbox row are saved together in one call,
 * which is the fix for the dual-write problem between Postgres and Kafka.
 */
class PostServiceTest {

    private PostRepository postRepository;
    private OutboxEventRepository outboxEventRepository;
    private PostService service;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        outboxEventRepository = mock(OutboxEventRepository.class);
        service = new PostService(postRepository, outboxEventRepository, new ObjectMapper(), mock(ExploreRanking.class));
    }

    @Test
    void createPostSavesThePostAndItsOutboxEventTogether() {
        UUID authorId = UUID.randomUUID();

        Post result = service.createPost(authorId, "hello world");

        assertEquals(authorId, result.getAuthorId());
        assertEquals("hello world", result.getBody());
        verify(postRepository).save(result);
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }
}

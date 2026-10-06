package com.showcase.feed.posts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showcase.feed.explore.ExploreRanking;
import com.showcase.feed.outbox.OutboxEvent;
import com.showcase.feed.outbox.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        Post result = service.createPost(authorId, "hello", "hello world", "http://img");

        assertEquals(authorId, result.getAuthorId());
        assertEquals("hello", result.getHeadline());
        assertEquals("hello world", result.getBody());
        assertEquals("http://img", result.getImageUrl());
        verify(postRepository).save(result);
        verify(outboxEventRepository).save(any(OutboxEvent.class));
    }

    @Test
    void attachImageSetsDataContentTypeAndImageUrlForOwnPost() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post existing = new Post();
        existing.setAuthorId(authorId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(existing));

        Post result = service.attachImage(postId, authorId, new byte[] {1, 2, 3}, "image/png",
            "http://host/posts/x/image");

        assertArrayEquals(new byte[] {1, 2, 3}, result.getImageData());
        assertEquals("image/png", result.getImageContentType());
        assertEquals("http://host/posts/x/image", result.getImageUrl());
        verify(postRepository).save(existing);
    }

    @Test
    void attachImageRejectsWhenCallerIsNotTheAuthor() {
        UUID authorId = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        Post existing = new Post();
        existing.setAuthorId(authorId);
        when(postRepository.findById(postId)).thenReturn(Optional.of(existing));

        assertThrows(NotPostAuthorException.class,
            () -> service.attachImage(postId, UUID.randomUUID(), new byte[] {1}, "image/png", "http://x"));
    }
}

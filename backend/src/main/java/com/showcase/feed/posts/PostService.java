package com.showcase.feed.posts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.showcase.feed.explore.ExploreRanking;
import com.showcase.feed.outbox.OutboxEvent;
import com.showcase.feed.outbox.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class PostService {
    private final PostRepository postRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ExploreRanking exploreRanking;

    public PostService(PostRepository postRepository,
                        OutboxEventRepository outboxEventRepository,
                        ObjectMapper objectMapper,
                        ExploreRanking exploreRanking) {
        this.postRepository = postRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.exploreRanking = exploreRanking;
    }

    /**
     * Post row and outbox row commit together in this one transaction, or neither does — that's
     * the dual-write fix: Kafka never finds out about a post Postgres rolled back.
     */
    @Transactional
    public Post createPost(UUID authorId, String headline, String body, String imageUrl) {
        Post post = new Post();
        post.setAuthorId(authorId);
        post.setHeadline(headline);
        post.setBody(body);
        post.setImageUrl(imageUrl);
        postRepository.save(post);

        OutboxEvent event = new OutboxEvent();
        event.setEventId(UUID.randomUUID());
        event.setAggregateType("post");
        event.setAggregateId(post.getId());
        event.setEventType("post.created");
        event.setPayload(writePayload(post));
        outboxEventRepository.save(event);

        exploreRanking.creditPost(authorId);
        return post;
    }

    private String writePayload(Post post) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("postId", post.getId());
        payload.put("authorId", post.getAuthorId());
        payload.put("createdAtEpochMilli", post.getCreatedAt().toEpochMilli());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to serialize post.created outbox payload", e);
        }
    }
}

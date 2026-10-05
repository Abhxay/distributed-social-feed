package com.showcase.feed.likes;

import com.showcase.feed.posts.PostRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LikeReconciliationJobTest {

    private StringRedisTemplate redis;
    private SetOperations<String, String> setOps;
    private ValueOperations<String, String> valueOps;
    private LikeRepository likeRepository;
    private PostRepository postRepository;
    private LikeReconciliationJob job;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        setOps = mock(SetOperations.class);
        valueOps = mock(ValueOperations.class);
        likeRepository = mock(LikeRepository.class);
        postRepository = mock(PostRepository.class);
        when(redis.opsForSet()).thenReturn(setOps);
        when(redis.opsForValue()).thenReturn(valueOps);
        job = new LikeReconciliationJob(redis, likeRepository, postRepository, new SimpleMeterRegistry());
    }

    @Test
    void reconcileRecomputesTrueCountAndSyncsBothCachesThenClearsDirtyEntry() {
        UUID postId = UUID.randomUUID();
        when(setOps.members("dirty:likes")).thenReturn(Set.of(postId.toString()));
        when(likeRepository.countByPostId(postId)).thenReturn(7L);

        job.reconcile();

        verify(postRepository).updateLikeCount(postId, 7L);
        verify(valueOps).set("likecount:" + postId, "7", Duration.ofMinutes(10));
        verify(setOps).remove("dirty:likes", postId.toString());
    }
}

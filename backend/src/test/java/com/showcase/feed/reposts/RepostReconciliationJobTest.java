package com.showcase.feed.reposts;

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

class RepostReconciliationJobTest {

    private StringRedisTemplate redis;
    private SetOperations<String, String> setOps;
    private ValueOperations<String, String> valueOps;
    private RepostRepository repostRepository;
    private PostRepository postRepository;
    private RepostReconciliationJob job;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        setOps = mock(SetOperations.class);
        valueOps = mock(ValueOperations.class);
        repostRepository = mock(RepostRepository.class);
        postRepository = mock(PostRepository.class);
        when(redis.opsForSet()).thenReturn(setOps);
        when(redis.opsForValue()).thenReturn(valueOps);
        job = new RepostReconciliationJob(redis, repostRepository, postRepository, new SimpleMeterRegistry());
    }

    @Test
    void reconcileRecomputesTrueCountAndSyncsBothCachesThenClearsDirtyEntry() {
        UUID postId = UUID.randomUUID();
        when(setOps.members("dirty:reposts")).thenReturn(Set.of(postId.toString()));
        when(repostRepository.countByPostId(postId)).thenReturn(4L);

        job.reconcile();

        verify(postRepository).updateRepostCount(postId, 4L);
        verify(valueOps).set("repostcount:" + postId, "4", Duration.ofMinutes(10));
        verify(setOps).remove("dirty:reposts", postId.toString());
    }
}

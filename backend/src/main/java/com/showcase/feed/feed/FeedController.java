package com.showcase.feed.feed;

import com.showcase.feed.likes.LikeRepository;
import com.showcase.feed.posts.Post;
import com.showcase.feed.posts.PostRepository;
import com.showcase.feed.posts.dto.PostResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/feed")
public class FeedController {
    private final FeedCacheService feedCacheService;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;

    public FeedController(FeedCacheService feedCacheService, PostRepository postRepository,
                           LikeRepository likeRepository) {
        this.feedCacheService = feedCacheService;
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
    }

    @GetMapping
    public List<PostResponse> getFeed(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        List<UUID> ids = feedCacheService.getFeed(userId);
        if (ids.isEmpty()) {
            return List.of();
        }

        // findAllById doesn't preserve order, so hydrate into a map and re-walk `ids` to restore it
        Map<UUID, Post> byId = new HashMap<>();
        for (Post post : postRepository.findAllById(ids)) {
            byId.put(post.getId(), post);
        }
        Set<UUID> likedPostIds = likeRepository.findLikedPostIds(userId, ids);

        return ids.stream()
            .map(byId::get)
            .filter(Objects::nonNull)
            .map(post -> new PostResponse(post.getId(), post.getAuthorId(), post.getBody(), post.getLikeCount(),
                likedPostIds.contains(post.getId()), post.getCreatedAt()))
            .toList();
    }
}

package com.showcase.feed.feed;

import com.showcase.feed.auth.User;
import com.showcase.feed.auth.UserRepository;
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
    private final UserRepository userRepository;

    public FeedController(FeedCacheService feedCacheService, PostRepository postRepository,
                           LikeRepository likeRepository, UserRepository userRepository) {
        this.feedCacheService = feedCacheService;
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
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

        Set<UUID> authorIds = byId.values().stream().map(Post::getAuthorId).collect(java.util.stream.Collectors.toSet());
        Map<UUID, String> usernameById = new HashMap<>();
        for (User user : userRepository.findAllById(authorIds)) {
            usernameById.put(user.getId(), user.getUsername());
        }

        return ids.stream()
            .map(byId::get)
            .filter(Objects::nonNull)
            .map(post -> new PostResponse(post.getId(), post.getAuthorId(),
                usernameById.getOrDefault(post.getAuthorId(), "unknown"), post.getBody(), post.getLikeCount(),
                likedPostIds.contains(post.getId()), post.getCreatedAt()))
            .toList();
    }
}

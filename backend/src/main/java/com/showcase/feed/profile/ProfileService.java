package com.showcase.feed.profile;

import com.showcase.feed.auth.User;
import com.showcase.feed.auth.UserRepository;
import com.showcase.feed.comments.CommentRepository;
import com.showcase.feed.explore.ExploreRanking;
import com.showcase.feed.posts.Post;
import com.showcase.feed.posts.PostRepository;
import com.showcase.feed.posts.dto.PostResponse;
import com.showcase.feed.reposts.Repost;
import com.showcase.feed.reposts.RepostRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProfileService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final RepostRepository repostRepository;
    private final ExploreRanking exploreRanking;

    public ProfileService(UserRepository userRepository, PostRepository postRepository,
                           CommentRepository commentRepository, RepostRepository repostRepository,
                           ExploreRanking exploreRanking) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.repostRepository = repostRepository;
        this.exploreRanking = exploreRanking;
    }

    public ProfileResponse getOwnProfile(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<Post> ownPosts = postRepository.findByAuthorIdOrderByCreatedAtDesc(userId);

        List<PostResponse> postResponses = ownPosts.stream()
            .map(p -> toResponse(p, user.getUsername(), false, false))
            .toList();

        List<Repost> ownReposts = repostRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<PostResponse> repostResponses = buildRepostResponses(ownReposts);

        long likesReceived = postRepository.sumLikesForAuthor(userId);
        long commentsReceived = commentRepository.countCommentsReceivedByAuthor(userId);
        double activityScore = exploreRanking.scoreOf(userId);

        return new ProfileResponse(user.getId(), user.getUsername(), ownPosts.size(), likesReceived,
            commentsReceived, activityScore, postResponses, repostResponses);
    }

    private List<PostResponse> buildRepostResponses(List<Repost> reposts) {
        if (reposts.isEmpty()) {
            return List.of();
        }
        List<UUID> postIds = reposts.stream().map(Repost::getPostId).toList();
        Map<UUID, Post> postById = new HashMap<>();
        for (Post post : postRepository.findAllById(postIds)) {
            postById.put(post.getId(), post);
        }
        Set<UUID> authorIds = postById.values().stream().map(Post::getAuthorId).collect(Collectors.toSet());
        Map<UUID, String> usernameById = new HashMap<>();
        for (User author : userRepository.findAllById(authorIds)) {
            usernameById.put(author.getId(), author.getUsername());
        }
        return postIds.stream()
            .map(postById::get)
            .filter(Objects::nonNull)
            .map(post -> toResponse(post, usernameById.getOrDefault(post.getAuthorId(), "unknown"), false, true))
            .toList();
    }

    private PostResponse toResponse(Post post, String authorUsername, boolean likedByMe, boolean repostedByMe) {
        return new PostResponse(post.getId(), post.getAuthorId(), authorUsername, post.getHeadline(), post.getBody(),
            post.getImageUrl(), post.getLikeCount(), likedByMe, post.getCommentCount(), post.getRepostCount(),
            repostedByMe, post.getCreatedAt());
    }
}

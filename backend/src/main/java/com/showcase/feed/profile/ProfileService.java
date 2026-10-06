package com.showcase.feed.profile;

import com.showcase.feed.auth.User;
import com.showcase.feed.auth.UserRepository;
import com.showcase.feed.comments.CommentRepository;
import com.showcase.feed.explore.ExploreRanking;
import com.showcase.feed.posts.Post;
import com.showcase.feed.posts.PostRepository;
import com.showcase.feed.posts.dto.PostResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ProfileService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ExploreRanking exploreRanking;

    public ProfileService(UserRepository userRepository, PostRepository postRepository,
                           CommentRepository commentRepository, ExploreRanking exploreRanking) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.exploreRanking = exploreRanking;
    }

    public ProfileResponse getOwnProfile(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        List<Post> ownPosts = postRepository.findByAuthorIdOrderByCreatedAtDesc(userId);

        List<PostResponse> postResponses = ownPosts.stream()
            .map(p -> new PostResponse(p.getId(), p.getAuthorId(), user.getUsername(), p.getBody(),
                p.getLikeCount(), false, p.getCreatedAt()))
            .toList();

        long likesReceived = postRepository.sumLikesForAuthor(userId);
        long commentsReceived = commentRepository.countCommentsReceivedByAuthor(userId);
        double activityScore = exploreRanking.scoreOf(userId);

        return new ProfileResponse(user.getId(), user.getUsername(), ownPosts.size(), likesReceived,
            commentsReceived, activityScore, postResponses);
    }
}

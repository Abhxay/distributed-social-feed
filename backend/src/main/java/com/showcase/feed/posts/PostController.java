package com.showcase.feed.posts;

import com.showcase.feed.auth.UserRepository;
import com.showcase.feed.common.idempotency.IdempotencyService;
import com.showcase.feed.likes.LikeRepository;
import com.showcase.feed.posts.dto.CreatePostRequest;
import com.showcase.feed.posts.dto.PostResponse;
import com.showcase.feed.reposts.RepostRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/posts")
public class PostController {
    private final PostService postService;
    private final IdempotencyService idempotencyService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final RepostRepository repostRepository;

    public PostController(PostService postService, IdempotencyService idempotencyService,
                           UserRepository userRepository, PostRepository postRepository,
                           LikeRepository likeRepository, RepostRepository repostRepository) {
        this.postService = postService;
        this.idempotencyService = idempotencyService;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.repostRepository = repostRepository;
    }

    // Idempotency-Key has no required=false: a missing required header is a 400 from Spring MVC itself.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                    @Valid @RequestBody CreatePostRequest request,
                                    Authentication authentication) {
        UUID authorId = UUID.fromString(authentication.getName());
        return idempotencyService.execute(idempotencyKey, request, PostResponse.class,
            () -> toResponse(postService.createPost(authorId, request.headline(), request.body(),
                request.imageUrl()), false, false));
    }

    @GetMapping("/{id}")
    public PostResponse getPost(@PathVariable UUID id, Authentication authentication) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new PostNotFoundException("post not found: " + id));
        UUID userId = UUID.fromString(authentication.getName());
        boolean likedByMe = !likeRepository.findLikedPostIds(userId, List.of(id)).isEmpty();
        boolean repostedByMe = !repostRepository.findRepostedPostIds(userId, List.of(id)).isEmpty();
        return toResponse(post, likedByMe, repostedByMe);
    }

    private PostResponse toResponse(Post post, boolean likedByMe, boolean repostedByMe) {
        String authorUsername = userRepository.findById(post.getAuthorId())
            .map(com.showcase.feed.auth.User::getUsername)
            .orElse("unknown");
        return new PostResponse(post.getId(), post.getAuthorId(), authorUsername, post.getHeadline(), post.getBody(),
            post.getImageUrl(), post.getLikeCount(), likedByMe, post.getCommentCount(), post.getRepostCount(),
            repostedByMe, post.getCreatedAt());
    }
}

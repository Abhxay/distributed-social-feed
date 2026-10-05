package com.showcase.feed.posts;

import com.showcase.feed.auth.UserRepository;
import com.showcase.feed.common.idempotency.IdempotencyService;
import com.showcase.feed.posts.dto.CreatePostRequest;
import com.showcase.feed.posts.dto.PostResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/posts")
public class PostController {
    private final PostService postService;
    private final IdempotencyService idempotencyService;
    private final UserRepository userRepository;

    public PostController(PostService postService, IdempotencyService idempotencyService,
                           UserRepository userRepository) {
        this.postService = postService;
        this.idempotencyService = idempotencyService;
        this.userRepository = userRepository;
    }

    // Idempotency-Key has no required=false: a missing required header is a 400 from Spring MVC itself.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                    @Valid @RequestBody CreatePostRequest request,
                                    Authentication authentication) {
        UUID authorId = UUID.fromString(authentication.getName());
        return idempotencyService.execute(idempotencyKey, request, PostResponse.class,
            () -> toResponse(postService.createPost(authorId, request.body())));
    }

    private PostResponse toResponse(Post post) {
        String authorUsername = userRepository.findById(post.getAuthorId())
            .map(com.showcase.feed.auth.User::getUsername)
            .orElse("unknown");
        return new PostResponse(post.getId(), post.getAuthorId(), authorUsername, post.getBody(),
            post.getLikeCount(), false, post.getCreatedAt());
    }
}

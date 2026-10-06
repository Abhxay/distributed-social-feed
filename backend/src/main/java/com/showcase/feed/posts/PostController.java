package com.showcase.feed.posts;

import com.showcase.feed.auth.UserRepository;
import com.showcase.feed.common.idempotency.IdempotencyService;
import com.showcase.feed.likes.LikeRepository;
import com.showcase.feed.posts.dto.CreatePostRequest;
import com.showcase.feed.posts.dto.PostResponse;
import com.showcase.feed.reposts.RepostRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/posts")
public class PostController {
    private static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;

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

    @PostMapping("/{id}/image")
    public PostResponse uploadImage(@PathVariable UUID id, @RequestParam("file") MultipartFile file,
                                     Authentication authentication, HttpServletRequest request) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new InvalidImageException("file must be an image");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new InvalidImageException("image must be 5MB or smaller");
        }
        UUID authorId = UUID.fromString(authentication.getName());
        String imageUrl = ServletUriComponentsBuilder.fromContextPath(request)
            .path("/posts/{id}/image").buildAndExpand(id).toUriString();
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new InvalidImageException("unable to read uploaded file");
        }
        Post post = postService.attachImage(id, authorId, data, contentType, imageUrl);
        boolean likedByMe = !likeRepository.findLikedPostIds(authorId, List.of(id)).isEmpty();
        boolean repostedByMe = !repostRepository.findRepostedPostIds(authorId, List.of(id)).isEmpty();
        return toResponse(post, likedByMe, repostedByMe);
    }

    // permitAll in SecurityConfig: a plain <img src> request can't carry an Authorization header.
    @GetMapping("/{id}/image")
    public ResponseEntity<byte[]> getImage(@PathVariable UUID id) {
        Post post = postRepository.findById(id)
            .orElseThrow(() -> new PostNotFoundException("post not found: " + id));
        if (post.getImageData() == null) {
            throw new PostNotFoundException("no image for post: " + id);
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, post.getImageContentType())
            .body(post.getImageData());
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

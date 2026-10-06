package com.showcase.feed.comments;

import com.showcase.feed.auth.User;
import com.showcase.feed.auth.UserRepository;
import com.showcase.feed.comments.dto.CommentRequest;
import com.showcase.feed.comments.dto.CommentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// No idempotency key here, unlike posts/likes: a duplicate comment on double-submit is a minor,
// visibly-correctable annoyance (not a double-charge-shaped risk), so the extra mechanism isn't
// earning its complexity for this one endpoint — a deliberate scope cut, not an oversight.
@RestController
@RequestMapping("/posts/{postId}/comments")
public class CommentController {
    private final CommentService commentService;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    public CommentController(CommentService commentService, CommentRepository commentRepository,
                              UserRepository userRepository) {
        this.commentService = commentService;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(@PathVariable UUID postId, @Valid @RequestBody CommentRequest request,
                                   Authentication authentication) {
        UUID authorId = UUID.fromString(authentication.getName());
        Comment comment = commentService.createComment(authorId, postId, request.body());
        String username = userRepository.findById(authorId).map(User::getUsername).orElse("unknown");
        return toResponse(comment, username);
    }

    @GetMapping
    public List<CommentResponse> list(@PathVariable UUID postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        if (comments.isEmpty()) {
            return List.of();
        }

        List<UUID> authorIds = comments.stream().map(Comment::getAuthorId).distinct().toList();
        Map<UUID, String> usernameById = new HashMap<>();
        for (User user : userRepository.findAllById(authorIds)) {
            usernameById.put(user.getId(), user.getUsername());
        }

        return comments.stream()
            .map(c -> toResponse(c, usernameById.getOrDefault(c.getAuthorId(), "unknown")))
            .toList();
    }

    private CommentResponse toResponse(Comment comment, String authorUsername) {
        return new CommentResponse(comment.getId(), comment.getPostId(), comment.getAuthorId(), authorUsername,
            comment.getBody(), comment.getCreatedAt());
    }
}

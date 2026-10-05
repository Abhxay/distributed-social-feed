package com.showcase.feed.likes;

import com.showcase.feed.common.idempotency.IdempotencyService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/posts/{id}/likes")
public class LikeController {
    private final LikeService likeService;
    private final IdempotencyService idempotencyService;

    public LikeController(LikeService likeService, IdempotencyService idempotencyService) {
        this.likeService = likeService;
        this.idempotencyService = idempotencyService;
    }

    // Idempotency-Key has no required=false: a missing required header is a 400 from Spring MVC itself.
    @PostMapping
    public LikeResponse like(@PathVariable("id") UUID postId,
                              @RequestHeader("Idempotency-Key") String idempotencyKey,
                              Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return idempotencyService.execute(idempotencyKey, postId, LikeResponse.class, () -> {
            likeService.like(userId, postId);
            return new LikeResponse(true);
        });
    }

    // No idempotency header: deleting a like that's already gone is naturally a no-op, always 200.
    @DeleteMapping
    public LikeResponse unlike(@PathVariable("id") UUID postId, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        likeService.unlike(userId, postId);
        return new LikeResponse(false);
    }
}

package com.showcase.feed.reposts;

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
@RequestMapping("/posts/{id}/reposts")
public class RepostController {
    private final RepostService repostService;
    private final IdempotencyService idempotencyService;

    public RepostController(RepostService repostService, IdempotencyService idempotencyService) {
        this.repostService = repostService;
        this.idempotencyService = idempotencyService;
    }

    // Idempotency-Key has no required=false: a missing required header is a 400 from Spring MVC itself.
    @PostMapping
    public RepostResponse repost(@PathVariable("id") UUID postId,
                                  @RequestHeader("Idempotency-Key") String idempotencyKey,
                                  Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return idempotencyService.execute(idempotencyKey, postId, RepostResponse.class, () -> {
            repostService.repost(userId, postId);
            return new RepostResponse(true);
        });
    }

    // No idempotency header: deleting a repost that's already gone is naturally a no-op, always 200.
    @DeleteMapping
    public RepostResponse unrepost(@PathVariable("id") UUID postId, Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        repostService.unrepost(userId, postId);
        return new RepostResponse(false);
    }
}

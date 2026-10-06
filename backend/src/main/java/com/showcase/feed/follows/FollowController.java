package com.showcase.feed.follows;

import com.showcase.feed.auth.UserRepository;
import com.showcase.feed.explore.ExplorePageResponse;
import com.showcase.feed.explore.ExploreService;
import com.showcase.feed.explore.ExploreUserResponse;
import com.showcase.feed.follows.dto.UserSearchResult;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class FollowController {
    private final FollowService followService;
    private final UserRepository userRepository;
    private final ExploreService exploreService;

    public FollowController(FollowService followService, UserRepository userRepository,
                             ExploreService exploreService) {
        this.followService = followService;
        this.userRepository = userRepository;
        this.exploreService = exploreService;
    }

    @PostMapping("/users/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@PathVariable("id") UUID followeeId, Authentication authentication) {
        followService.follow(UUID.fromString(authentication.getName()), followeeId);
    }

    @DeleteMapping("/users/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@PathVariable("id") UUID followeeId, Authentication authentication) {
        followService.unfollow(UUID.fromString(authentication.getName()), followeeId);
    }

    // Maps to a minimal DTO rather than returning the auth.User entity directly — that entity
    // exposes getPasswordHash(), which Jackson would otherwise serialize into this response.
    @GetMapping("/users/search")
    public List<UserSearchResult> search(@RequestParam("q") String q) {
        return userRepository.findTop10ByUsernameContainingIgnoreCase(q).stream()
            .map(u -> new UserSearchResult(u.getId(), u.getUsername()))
            .toList();
    }

    // Ranked by activity score (posting + being liked/commented on), read from a Redis sorted set
    // — see ExploreRanking for why this one piece of data lives natively in Redis rather than
    // Postgres. Offset-paginated directly against the sorted set (ZREVRANGE), not a full fetch, so
    // the frontend can page through in small batches cheaply.
    @GetMapping("/users/explore")
    public ExplorePageResponse explore(@RequestParam(defaultValue = "0") int offset,
                                        @RequestParam(defaultValue = "5") int limit,
                                        Authentication authentication) {
        UUID currentUserId = UUID.fromString(authentication.getName());
        List<ExploreUserResponse> users = exploreService.getRanking(currentUserId, offset, limit);
        long total = exploreService.totalRanked();
        return new ExplorePageResponse(users, total, offset + limit < total);
    }
}

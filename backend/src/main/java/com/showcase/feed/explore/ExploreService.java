package com.showcase.feed.explore;

import com.showcase.feed.auth.User;
import com.showcase.feed.auth.UserRepository;
import com.showcase.feed.follows.FollowRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ExploreService {
    private final ExploreRanking exploreRanking;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    public ExploreService(ExploreRanking exploreRanking, UserRepository userRepository,
                           FollowRepository followRepository) {
        this.exploreRanking = exploreRanking;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    public List<ExploreUserResponse> getRanking(UUID currentUserId, int limit) {
        List<ExploreRanking.RankedUser> ranked = exploreRanking.topUsers(limit);
        if (ranked.isEmpty()) {
            return List.of();
        }

        List<UUID> candidateIds = ranked.stream().map(ExploreRanking.RankedUser::userId).toList();

        Map<UUID, String> usernameById = new HashMap<>();
        for (User user : userRepository.findAllById(candidateIds)) {
            usernameById.put(user.getId(), user.getUsername());
        }

        Set<UUID> alreadyFollowing = followRepository.findFollowedIds(currentUserId, candidateIds);

        return ranked.stream()
            .filter(entry -> usernameById.containsKey(entry.userId())) // skip any deleted/orphaned ranking entries
            .map(entry -> new ExploreUserResponse(
                entry.userId(),
                usernameById.get(entry.userId()),
                entry.activityScore(),
                alreadyFollowing.contains(entry.userId())))
            .toList();
    }
}

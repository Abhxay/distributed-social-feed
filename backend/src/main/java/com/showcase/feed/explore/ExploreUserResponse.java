package com.showcase.feed.explore;

import java.util.UUID;

public record ExploreUserResponse(UUID id, String username, double activityScore, boolean isFollowing) {}

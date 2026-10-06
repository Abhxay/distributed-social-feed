package com.showcase.feed.explore;

import java.util.List;

public record ExplorePageResponse(List<ExploreUserResponse> users, long totalRanked, boolean hasMore) {}

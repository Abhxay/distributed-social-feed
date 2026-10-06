package com.showcase.feed.profile;

import com.showcase.feed.posts.dto.PostResponse;

import java.util.List;
import java.util.UUID;

public record ProfileResponse(UUID id, String username, long postCount, long likesReceived,
                               long commentsReceived, double activityScore, List<PostResponse> posts) {}

package com.showcase.feed.posts.dto;

import java.time.Instant;
import java.util.UUID;

public record PostResponse(
    UUID postId,
    UUID authorId,
    String authorUsername,
    String body,
    int likeCount,
    boolean likedByMe,
    Instant createdAt
) {}

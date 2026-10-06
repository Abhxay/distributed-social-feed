package com.showcase.feed.posts.dto;

import java.time.Instant;
import java.util.UUID;

public record PostResponse(
    UUID postId,
    UUID authorId,
    String authorUsername,
    String headline,
    String body,
    String imageUrl,
    int likeCount,
    boolean likedByMe,
    int commentCount,
    int repostCount,
    boolean repostedByMe,
    Instant createdAt
) {}

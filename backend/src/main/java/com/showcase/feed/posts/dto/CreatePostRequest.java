package com.showcase.feed.posts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
    @NotBlank @Size(max = 150) String headline,
    @NotBlank @Size(max = 2000) String body,
    @Size(max = 500) String imageUrl
) {}

package com.showcase.feed.posts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(@NotBlank @Size(max = 280) String body) {}

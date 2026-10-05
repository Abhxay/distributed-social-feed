package com.showcase.feed.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
    @NotBlank @Size(max = 30) String username,
    @NotBlank String password
) {}

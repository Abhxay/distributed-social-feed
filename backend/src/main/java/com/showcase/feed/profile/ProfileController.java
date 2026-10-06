package com.showcase.feed.profile;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ProfileController {
    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/users/me")
    public ProfileResponse me(Authentication authentication) {
        return profileService.getOwnProfile(UUID.fromString(authentication.getName()));
    }
}

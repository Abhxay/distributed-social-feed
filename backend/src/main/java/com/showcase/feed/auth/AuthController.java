package com.showcase.feed.auth;

import com.showcase.feed.auth.dto.ChangePasswordRequest;
import com.showcase.feed.auth.dto.LoginRequest;
import com.showcase.feed.auth.dto.RefreshRequest;
import com.showcase.feed.auth.dto.SignupRequest;
import com.showcase.feed.auth.dto.TokenPairResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final UsernameAvailabilityService usernameAvailabilityService;
    private final RefreshTokenService refreshTokenService;

    public AuthController(AuthService authService,
                           UsernameAvailabilityService usernameAvailabilityService,
                           RefreshTokenService refreshTokenService) {
        this.authService = authService;
        this.usernameAvailabilityService = usernameAvailabilityService;
        this.refreshTokenService = refreshTokenService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, UUID> signup(@Valid @RequestBody SignupRequest request) {
        UUID userId = authService.signup(request.username(), request.password());
        return Map.of("userId", userId);
    }

    @PostMapping("/login")
    public TokenPairResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    @GetMapping("/check-username")
    public Map<String, Boolean> checkUsername(@RequestParam String username) {
        return Map.of("available", usernameAvailabilityService.isAvailable(username));
    }

    @PostMapping("/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshTokenPair pair = refreshTokenService.rotate(request.refreshToken());
        return new TokenPairResponse(pair.accessToken(), pair.rawRefreshToken());
    }

    @PostMapping("/change-password")
    public Map<String, Boolean> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                                Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        authService.changePassword(userId, request.currentPassword(), request.newPassword());
        return Map.of("success", true);
    }
}

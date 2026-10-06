package com.showcase.feed.auth;

import com.showcase.feed.auth.dto.TokenPairResponse;
import com.showcase.feed.common.bloom.UsernameBloomFilter;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsernameBloomFilter usernameBloomFilter;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        UsernameBloomFilter usernameBloomFilter,
                        RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.usernameBloomFilter = usernameBloomFilter;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public UUID signup(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new UsernameTakenException(username);
        }
        User user = new User(username, passwordEncoder.encode(rawPassword));
        userRepository.save(user);
        usernameBloomFilter.add(username);
        return user.getId();
    }

    public TokenPairResponse login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new InvalidTokenException("invalid credentials"));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidTokenException("invalid credentials");
        }
        RefreshTokenPair pair = refreshTokenService.issue(user.getId());
        return new TokenPairResponse(pair.accessToken(), pair.rawRefreshToken());
    }

    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new InvalidTokenException("unknown user"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IncorrectPasswordException();
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}

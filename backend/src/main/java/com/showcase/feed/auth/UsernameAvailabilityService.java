package com.showcase.feed.auth;

import com.showcase.feed.common.bloom.UsernameBloomFilter;
import org.springframework.stereotype.Service;

@Service
public class UsernameAvailabilityService {
    private final UsernameBloomFilter bloomFilter;
    private final UserRepository userRepository;

    public UsernameAvailabilityService(UsernameBloomFilter bloomFilter, UserRepository userRepository) {
        this.bloomFilter = bloomFilter;
        this.userRepository = userRepository;
    }

    /**
     * Deliberately demonstrates the Bloom-filter pre-check pattern. At this project's scale a plain
     * indexed query against the DB UNIQUE(username) constraint would already be fast enough on its own —
     * the DB remains authoritative either way. This exists to show the technique, not because it's load-bearing.
     */
    public boolean isAvailable(String username) {
        if (!bloomFilter.mightContain(username)) return true;
        return !userRepository.existsByUsername(username);
    }
}

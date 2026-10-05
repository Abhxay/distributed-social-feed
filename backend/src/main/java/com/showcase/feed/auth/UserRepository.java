package com.showcase.feed.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);

    /** Used by the follow-search endpoint; capped at 10 results so a broad query can't scan the whole table. */
    List<User> findTop10ByUsernameContainingIgnoreCase(String query);
}

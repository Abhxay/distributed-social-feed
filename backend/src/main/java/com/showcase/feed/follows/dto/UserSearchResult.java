package com.showcase.feed.follows.dto;

import java.util.UUID;

/**
 * Deliberately not the raw com.showcase.feed.auth.User entity: that entity exposes
 * getPasswordHash(), which Jackson would happily serialize straight into this public search
 * response. This is the minimal safe projection instead.
 */
public record UserSearchResult(UUID id, String username) {}

package com.showcase.feed.follows;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Id class for Follow's composite primary key (follower_id, followee_id) — used with @IdClass. */
public class FollowId implements Serializable {
    private UUID followerId;
    private UUID followeeId;

    public FollowId() {
    }

    public FollowId(UUID followerId, UUID followeeId) {
        this.followerId = followerId;
        this.followeeId = followeeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FollowId that)) return false;
        return Objects.equals(followerId, that.followerId) && Objects.equals(followeeId, that.followeeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(followerId, followeeId);
    }
}

package com.showcase.feed.likes;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Id class for Like's composite primary key (user_id, post_id) — used with @IdClass. */
public class LikeId implements Serializable {
    private UUID userId;
    private UUID postId;

    public LikeId() {
    }

    public LikeId(UUID userId, UUID postId) {
        this.userId = userId;
        this.postId = postId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LikeId that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(postId, that.postId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, postId);
    }
}

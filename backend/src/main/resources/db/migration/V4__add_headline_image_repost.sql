ALTER TABLE posts ADD COLUMN headline VARCHAR(150) NOT NULL DEFAULT '';
ALTER TABLE posts ADD COLUMN image_url VARCHAR(500);
ALTER TABLE posts ADD COLUMN repost_count INT NOT NULL DEFAULT 0;
ALTER TABLE posts ADD COLUMN comment_count INT NOT NULL DEFAULT 0;

CREATE TABLE reposts (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, post_id)
);
CREATE INDEX idx_reposts_post ON reposts(post_id);
CREATE INDEX idx_reposts_user_created ON reposts(user_id, created_at DESC);

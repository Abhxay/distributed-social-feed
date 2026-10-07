-- Supports the feed's discovery query (recent-or-well-liked posts, regardless of follow),
-- which orders by created_at with no author_id filter — idx_posts_author_created doesn't help there.
CREATE INDEX idx_posts_created ON posts(created_at DESC);

import { useState } from 'react';
import { useExploreUsersQuery, useFollowMutation, useUnfollowMutation } from '../../api/apiSlice';
import { Header } from '../../components/Header';

const PAGE_SIZE = 5;

export function ExplorePage() {
  const [offset, setOffset] = useState(0);
  const [direction, setDirection] = useState<'forward' | 'back'>('forward');
  // RTK Query caches each {offset, limit} combination separately — going back to an offset
  // already seen is served instantly from cache, no network call. Only moving forward into a
  // new offset for the first time triggers a real fetch.
  const { data, isFetching } = useExploreUsersQuery({ offset, limit: PAGE_SIZE });
  const [follow] = useFollowMutation();
  const [unfollow] = useUnfollowMutation();

  const users = data?.users ?? [];
  const hasMore = data?.hasMore ?? false;

  const goForward = () => {
    setDirection('forward');
    setOffset((o) => o + PAGE_SIZE);
  };
  const goBack = () => {
    setDirection('back');
    setOffset((o) => Math.max(0, o - PAGE_SIZE));
  };

  return (
    <div className="feed-page">
      <Header />
      <main className="feed-main">
        <div className="explore-intro">
          <h1>Explore Creators & Engineers</h1>
          <p className="muted">
            Ranked by weighted activity (posts + likes + comments received) via Redis sorted set.
          </p>
        </div>

        <div className="explore-pager">
          {offset > 0 && (
            <button type="button" className="explore-arrow explore-arrow-left" onClick={goBack} aria-label="Previous">
              ‹
            </button>
          )}

          <ul key={offset} className={`explore-list explore-slide-${direction}`}>
            {users.map((user, index) => (
              <li key={user.id} className="explore-row">
                <span className="explore-rank">#{offset + index + 1}</span>
                <div className="feed-post-avatar" aria-hidden="true">
                  {user.username.slice(0, 1).toUpperCase()}
                </div>
                <div className="explore-user-info">
                  <p className="feed-post-author">{user.username}</p>
                  <p className="muted explore-score">{user.activityScore} activity points</p>
                </div>
                <button
                  type="button"
                  className={user.isFollowing ? 'ghost-button' : ''}
                  onClick={() => (user.isFollowing ? unfollow(user.id) : follow(user.id))}
                >
                  {user.isFollowing ? 'Unfollow' : 'Follow'}
                </button>
              </li>
            ))}
            {!isFetching && users.length === 0 && (
              <p className="muted">No activity yet. Be the first to post something.</p>
            )}
          </ul>
        </div>

        <div className="explore-footer">
          <span className="muted tabular-nums">
            Batch {Math.floor(offset / PAGE_SIZE) + 1} · {users.length} accounts shown
          </span>
          {hasMore ? (
            <button type="button" className="ghost-button" onClick={goForward} disabled={isFetching}>
              {isFetching ? 'Loading…' : 'Explore more'}
            </button>
          ) : (
            <span className="muted">All ranked accounts viewed</span>
          )}
        </div>
      </main>
    </div>
  );
}

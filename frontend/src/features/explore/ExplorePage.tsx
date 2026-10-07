import { useEffect, useState } from 'react';
import { apiSlice, useExploreUsersQuery, useFollowMutation, useUnfollowMutation } from '../../api/apiSlice';
import { Header } from '../../components/Header';

const PAGE_SIZE = 5;

export function ExplorePage() {
  const [offset, setOffset] = useState(0);
  // set on every forward move, cleared on back - the blurred peek only ever shows
  // the batch you're moving away from, never the one you're returning to
  const [previousOffset, setPreviousOffset] = useState<number | null>(null);
  // separate from previousOffset - this only drives which slide-in animation plays
  const [direction, setDirection] = useState<'forward' | 'back'>('forward');

  const { data, isFetching } = useExploreUsersQuery({ offset, limit: PAGE_SIZE });
  const { data: previousData } = useExploreUsersQuery(
    { offset: previousOffset ?? 0, limit: PAGE_SIZE },
    // already in cache from when it was the current batch - this never hits the network
    { skip: previousOffset === null },
  );
  const [follow] = useFollowMutation();
  const [unfollow] = useUnfollowMutation();

  const users = data?.users ?? [];
  const hasMore = data?.hasMore ?? false;
  const previousUsers = previousData?.users ?? [];

  const prefetchExplore = apiSlice.usePrefetch('exploreUsers');
  useEffect(() => {
    // fire as soon as this batch is showing, not on hover - by the time someone reads
    // 5 names and decides to click "Explore more", the next batch is usually already cached
    if (hasMore) prefetchExplore({ offset: offset + PAGE_SIZE, limit: PAGE_SIZE });
  }, [offset, hasMore, prefetchExplore]);

  const goForward = () => {
    setDirection('forward');
    setPreviousOffset(offset);
    setOffset((o) => o + PAGE_SIZE);
  };
  const goBack = () => {
    setDirection('back');
    setPreviousOffset(null);
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

          <div className="explore-carousel">
            {previousOffset !== null && previousUsers.length > 0 && (
              <ul
                className="explore-list explore-list-peek"
                onClick={goBack}
                aria-hidden="true"
              >
                {previousUsers.map((user, index) => (
                  <li key={user.id} className="explore-row">
                    <span className="explore-rank">#{previousOffset + index + 1}</span>
                    <div className="feed-post-avatar" aria-hidden="true">
                      {user.username.slice(0, 1).toUpperCase()}
                    </div>
                    <div className="explore-user-info">
                      <p className="feed-post-author">{user.username}</p>
                      <p className="muted explore-score">{user.activityScore} activity points</p>
                    </div>
                  </li>
                ))}
              </ul>
            )}

            <ul key={offset} className={`explore-list explore-list-current explore-slide-${direction}`}>
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

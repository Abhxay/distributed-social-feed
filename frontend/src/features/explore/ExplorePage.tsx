import { useExploreUsersQuery, useFollowMutation, useUnfollowMutation } from '../../api/apiSlice';
import { Header } from '../../components/Header';

export function ExplorePage() {
  const { data: users = [], isLoading } = useExploreUsersQuery();
  const [follow] = useFollowMutation();
  const [unfollow] = useUnfollowMutation();

  return (
    <div className="feed-page">
      <Header />

      <main className="feed-main">
        <div className="explore-intro">
          <h1>Explore</h1>
          <p className="muted">
            Ranked by activity — posting and being liked both count, likes count double.
          </p>
        </div>

        {isLoading && <p className="muted">Loading…</p>}
        {!isLoading && users.length === 0 && (
          <p className="muted">No activity yet — be the first to post something.</p>
        )}

        <ul className="explore-list">
          {users.map((user, index) => (
            <li key={user.id} className="explore-row">
              <span className="explore-rank">{index + 1}</span>
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
        </ul>
      </main>
    </div>
  );
}

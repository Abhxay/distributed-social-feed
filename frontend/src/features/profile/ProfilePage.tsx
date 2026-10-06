import { useGetProfileQuery } from '../../api/apiSlice';
import { Header } from '../../components/Header';

export function ProfilePage() {
  const { data: profile, isLoading } = useGetProfileQuery();

  return (
    <div className="feed-page">
      <Header />
      <main className="feed-main">
        {isLoading && <p className="muted">Loading…</p>}
        {profile && (
          <>
            <div className="profile-summary">
              <div className="feed-post-avatar" aria-hidden="true">
                {profile.username.slice(0, 1).toUpperCase()}
              </div>
              <div>
                <h1>{profile.username}</h1>
                <p className="muted">
                  {profile.postCount} posts · {profile.likesReceived} likes received ·{' '}
                  {profile.commentsReceived} comments received · {profile.activityScore} activity points
                </p>
              </div>
            </div>

            <ul className="feed-list">
              {profile.posts.map((post) => (
                <li key={post.postId} className="feed-post">
                  <div className="feed-post-content">
                    <p className="feed-post-body">{post.body}</p>
                    <p className="muted explore-score">
                      {post.likeCount} likes · {new Date(post.createdAt).toLocaleString()}
                    </p>
                  </div>
                </li>
              ))}
              {profile.posts.length === 0 && <p className="muted">You haven't posted anything yet.</p>}
            </ul>
          </>
        )}
      </main>
    </div>
  );
}

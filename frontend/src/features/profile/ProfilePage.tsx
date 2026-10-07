import { useState } from 'react';
import { useGetProfileQuery } from '../../api/apiSlice';
import { Header } from '../../components/Header';
import { PostCard } from '../feed/PostCard';
import { PostExpandedOverlay } from '../feed/PostExpandedOverlay';

export function ProfilePage() {
  const { data: profile, isLoading } = useGetProfileQuery();
  const [activeTab, setActiveTab] = useState<'posts' | 'reposts'>('posts');
  const [expandedPostId, setExpandedPostId] = useState<string | null>(null);
  const expandedPost =
    profile?.posts.find((p) => p.postId === expandedPostId) ??
    profile?.reposts.find((p) => p.postId === expandedPostId) ??
    null;

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
              </div>
            </div>

            <div className="profile-stats">
              <div className="profile-stat">
                <div className="muted profile-stat-label">Posts authored</div>
                <div className="profile-stat-value tabular-nums">{profile.postCount}</div>
              </div>
              <div className="profile-stat">
                <div className="muted profile-stat-label">Likes received</div>
                <div className="profile-stat-value tabular-nums">{profile.likesReceived}</div>
              </div>
              <div className="profile-stat">
                <div className="muted profile-stat-label">Comments received</div>
                <div className="profile-stat-value tabular-nums">{profile.commentsReceived}</div>
              </div>
              <div className="profile-stat">
                <div className="muted profile-stat-label">Activity score</div>
                <div className="profile-stat-value tabular-nums">{profile.activityScore}</div>
              </div>
              {/* ponytail: followersCount has no real backend field yet — skipped rather than faked */}
            </div>

            <div className="tab-group">
              <button
                type="button"
                className={activeTab === 'posts' ? 'tab-button active' : 'tab-button'}
                onClick={() => setActiveTab('posts')}
              >
                Your posts ({profile.posts.length})
              </button>
              <button
                type="button"
                className={activeTab === 'reposts' ? 'tab-button active' : 'tab-button'}
                onClick={() => setActiveTab('reposts')}
              >
                Reposts ({profile.reposts.length})
              </button>
            </div>

            {activeTab === 'posts' ? (
              <ul className="feed-list">
                {profile.posts.map((post) => (
                  <PostCard key={post.postId} post={post} onCardClick={() => setExpandedPostId(post.postId)} />
                ))}
                {profile.posts.length === 0 && <p className="muted">No authored posts yet.</p>}
              </ul>
            ) : profile.reposts.length === 0 ? (
              <p className="muted">No reposted content yet.</p>
            ) : (
              <div className="feed-list">
                {profile.reposts.map((post) => (
                  <div key={`repost-${post.postId}`} className="repost-wrapper">
                    <div className="repost-label">↻ Reposted by @{profile.username}</div>
                    <ul className="feed-list">
                      <PostCard post={post} onCardClick={() => setExpandedPostId(post.postId)} />
                    </ul>
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </main>

      <PostExpandedOverlay post={expandedPost} onClose={() => setExpandedPostId(null)} />
    </div>
  );
}

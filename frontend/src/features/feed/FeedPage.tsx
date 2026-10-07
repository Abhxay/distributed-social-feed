import { useNavigate } from 'react-router-dom';
import {
  useGetFeedQuery,
  useLikeMutation,
  useRepostPostMutation,
  useUnlikeMutation,
  useUnrepostPostMutation,
} from '../../api/apiSlice';
import type { Post } from '../../api/apiSlice';
import { Header } from '../../components/Header';
import { TrendingPanel } from '../../components/TrendingPanel';
import { useFeedKeyboardNavigation } from '../../hooks/useFeedKeyboardNavigation';
import { FollowSearch } from '../search/FollowSearch';
import { PostCard } from './PostCard';
import { PostComposer } from './PostComposer';

export function FeedPage() {
  const { data: posts = [], isLoading } = useGetFeedQuery();
  const navigate = useNavigate();
  const [like] = useLikeMutation();
  const [unlike] = useUnlikeMutation();
  const [repostPost] = useRepostPostMutation();
  const [unrepostPost] = useUnrepostPostMutation();

  const { activePostId, setActiveIndex, registerPostRef } = useFeedKeyboardNavigation({
    posts,
    onOpenPost: (post: Post) => navigate(`/posts/${post.postId}`),
    onToggleLike: (post: Post) => (post.likedByMe ? unlike(post.postId) : like(post.postId)),
    onToggleRepost: (post: Post) =>
      post.repostedByMe ? unrepostPost(post.postId) : repostPost(post.postId),
  });

  return (
    <div className="feed-page">
      <Header />
      <FollowSearch />

      <div className="feed-layout">
        <TrendingPanel />

        <main className="feed-main">
          {isLoading && <p className="muted">Loading feed…</p>}
          {!isLoading && posts.length === 0 && (
            <p className="muted">
              Nothing here yet. Follow someone, or write the first post.
            </p>
          )}

          <ul className="feed-list">
            {posts.map((post, index) => (
              <PostCard
                key={post.postId}
                post={post}
                isActive={post.postId === activePostId}
                cardRef={(el) => registerPostRef(index, el)}
                onCardClick={() => setActiveIndex(index)}
              />
            ))}
          </ul>
        </main>

        <aside className="feed-sidebar composer-panel">
          <h2>Share something</h2>
          <PostComposer />
        </aside>
      </div>
    </div>
  );
}

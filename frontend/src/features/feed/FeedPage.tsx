import { useGetFeedQuery, useLikeMutation, useUnlikeMutation } from '../../api/apiSlice';
import { Header } from '../../components/Header';
import { FollowSearch } from '../search/FollowSearch';
import { PostComments } from './PostComments';
import { PostComposer } from './PostComposer';

export function FeedPage() {
  const { data: posts = [], isLoading } = useGetFeedQuery();
  const [like] = useLikeMutation();
  const [unlike] = useUnlikeMutation();

  return (
    <div className="feed-page">
      <Header />

      <main className="feed-main">
        <FollowSearch />
        <PostComposer />

        {isLoading && <p className="muted">Loading feed…</p>}
        {!isLoading && posts.length === 0 && (
          <p className="muted">
            Nothing here yet — follow someone above, or write the first post.
          </p>
        )}

        <ul className="feed-list">
          {posts.map((post) => (
            <li key={post.postId} className="feed-post">
              <div className="feed-post-avatar" aria-hidden="true">
                {(post.authorUsername || '?').slice(0, 1).toUpperCase()}
              </div>
              <div className="feed-post-content">
                <p className="feed-post-author">{post.authorUsername}</p>
                <p className="feed-post-body">{post.body}</p>
                <button
                  type="button"
                  className={post.likedByMe ? 'like-button liked' : 'like-button'}
                  onClick={() => (post.likedByMe ? unlike(post.postId) : like(post.postId))}
                >
                  {post.likedByMe ? '♥' : '♡'} {post.likeCount}
                </button>
                <PostComments postId={post.postId} />
              </div>
            </li>
          ))}
        </ul>
      </main>
    </div>
  );
}

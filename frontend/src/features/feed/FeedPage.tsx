import { useGetFeedQuery, useLikeMutation, useUnlikeMutation } from '../../api/apiSlice';
import { PostComposer } from './PostComposer';

export function FeedPage() {
  const { data: posts = [], isLoading } = useGetFeedQuery();
  const [like] = useLikeMutation();
  const [unlike] = useUnlikeMutation();

  return (
    <div className="feed-page">
      <PostComposer />

      {isLoading && <p>Loading feed…</p>}

      <ul className="feed-list">
        {posts.map((post) => (
          <li key={post.postId} className="feed-post">
            <p className="feed-post-author">{post.authorId}</p>
            <p className="feed-post-body">{post.body}</p>
            <button
              type="button"
              onClick={() => (post.likedByMe ? unlike(post.postId) : like(post.postId))}
            >
              {post.likedByMe ? 'Unlike' : 'Like'} ({post.likeCount})
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}

import type { Post } from '../../api/apiSlice';
import { useLikeMutation, useRepostPostMutation, useUnlikeMutation, useUnrepostPostMutation } from '../../api/apiSlice';
import { HeartIcon, RepostIcon } from '../../components/Icons';
import { PostComments } from './PostComments';

export function PostExpanded({ post }: { post: Post }) {
  const [like] = useLikeMutation();
  const [unlike] = useUnlikeMutation();
  const [repostPost] = useRepostPostMutation();
  const [unrepostPost] = useUnrepostPostMutation();

  return (
    <div className="post-expanded">
      <div className="post-expanded-comments">
        <PostComments postId={post.postId} />
      </div>

      <div className="post-expanded-main">
        <div className="feed-post-author-row">
          <div className="feed-post-avatar" aria-hidden="true">
            {(post.authorUsername || '?').slice(0, 1).toUpperCase()}
          </div>
          <p className="feed-post-author">{post.authorUsername}</p>
        </div>

        <h2 className="post-headline post-headline-expanded">{post.headline}</h2>

        {post.imageUrl && (
          <img className="post-image" src={post.imageUrl} alt={post.headline} loading="lazy" />
        )}

        <p className="feed-post-body">{post.body}</p>

        <p className="muted post-counts tabular-nums">
          {post.likeCount} likes · {post.commentCount} comments · {post.repostCount} reposts
        </p>

        <div className="post-actions">
          <button
            type="button"
            className={post.likedByMe ? 'like-button liked' : 'like-button'}
            onClick={() => (post.likedByMe ? unlike(post.postId) : like(post.postId))}
          >
            <HeartIcon size={15} filled={post.likedByMe} /> {post.likeCount}
          </button>
          <button
            type="button"
            className={post.repostedByMe ? 'repost-button reposted' : 'repost-button'}
            aria-label={post.repostedByMe ? 'Undo repost' : 'Repost'}
            onClick={() => (post.repostedByMe ? unrepostPost(post.postId) : repostPost(post.postId))}
          >
            <RepostIcon size={15} /> {post.repostCount}
          </button>
        </div>
      </div>
    </div>
  );
}

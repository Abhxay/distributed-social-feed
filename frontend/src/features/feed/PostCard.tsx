import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link } from 'react-router-dom';
import type { Post } from '../../api/apiSlice';
import {
  useCreateCommentMutation,
  useLikeMutation,
  useRepostPostMutation,
  useUnlikeMutation,
  useUnrepostPostMutation,
} from '../../api/apiSlice';
import { truncateWords } from '../../utils/truncateWords';
import { CommentIcon, HeartIcon, RepostIcon } from '../../components/Icons';

const BODY_WORD_LIMIT = 200;

interface PostCardProps {
  post: Post;
  truncateBody?: boolean;
  isActive?: boolean;
  cardRef?: (node: HTMLElement | null) => void;
  onCardClick?: () => void;
}

export function PostCard({ post, truncateBody = true, isActive = false, cardRef, onCardClick }: PostCardProps) {
  const fullView = !truncateBody;
  const [like] = useLikeMutation();
  const [unlike] = useUnlikeMutation();
  const [repostPost] = useRepostPostMutation();
  const [unrepostPost] = useUnrepostPostMutation();
  const [createComment, { isLoading: isCommenting }] = useCreateCommentMutation();

  const [quickCommentOpen, setQuickCommentOpen] = useState(false);
  const [commentBody, setCommentBody] = useState('');

  const { text: bodyText, truncated } = truncateBody
    ? truncateWords(post.body, BODY_WORD_LIMIT)
    : { text: post.body, truncated: false };

  const handleQuickComment = async (e: FormEvent) => {
    e.preventDefault();
    if (!commentBody.trim()) return;
    await createComment({ postId: post.postId, body: commentBody.trim() });
    setCommentBody('');
  };

  return (
    <li
      ref={cardRef}
      onClick={onCardClick}
      className={isActive ? 'feed-post feed-post-active' : 'feed-post'}
    >
      <div className="feed-post-avatar" aria-hidden="true">
        {(post.authorUsername || '?').slice(0, 1).toUpperCase()}
      </div>
      <div className="feed-post-content">
        <div className="feed-post-author-row">
          <p className="feed-post-author">{post.authorUsername}</p>
          {isActive && <span className="feed-post-hint">Enter to open · L like</span>}
        </div>
        <h3 className="post-headline">{post.headline}</h3>

        {post.imageUrl && (
          <img className="post-image" src={post.imageUrl} alt={post.headline} loading="lazy" />
        )}

        <p className="feed-post-body">
          {bodyText}
          {truncated && (
            <>
              {' '}
              <Link to={`/posts/${post.postId}`} className="read-more-link">
                Read more
              </Link>
            </>
          )}
        </p>

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
          <button
            type="button"
            className="comment-button"
            disabled={fullView}
            onClick={fullView ? undefined : () => setQuickCommentOpen((v) => !v)}
          >
            <CommentIcon size={15} /> Comment
          </button>
        </div>

        {!fullView && quickCommentOpen && (
          <div className="quick-comment">
            <form onSubmit={handleQuickComment} className="comment-form">
              <input
                aria-label="Write a comment"
                value={commentBody}
                onChange={(e) => setCommentBody(e.target.value)}
                placeholder="Write a comment…"
              />
              <button type="submit" disabled={isCommenting || !commentBody.trim()}>
                Reply
              </button>
            </form>
            <Link to={`/posts/${post.postId}`} className="view-thread-link">
              View full comment thread →
            </Link>
          </div>
        )}
      </div>
    </li>
  );
}

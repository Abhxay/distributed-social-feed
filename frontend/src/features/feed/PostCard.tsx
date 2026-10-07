import type { Post } from '../../api/apiSlice';

interface PostCardProps {
  post: Post;
  isActive?: boolean;
  cardRef?: (node: HTMLElement | null) => void;
  onCardClick?: () => void;
}

export function PostCard({ post, isActive = false, cardRef, onCardClick }: PostCardProps) {
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
      </div>
    </li>
  );
}

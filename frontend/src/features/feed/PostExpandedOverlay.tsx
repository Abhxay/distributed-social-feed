import { useEffect } from 'react';
import type { Post } from '../../api/apiSlice';
import { CloseIcon } from '../../components/Icons';
import { PostExpanded } from './PostExpanded';

export function PostExpandedOverlay({ post, onClose }: { post: Post | null; onClose: () => void }) {
  useEffect(() => {
    if (!post) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [post, onClose]);

  if (!post) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="post-expanded-panel" onClick={(e) => e.stopPropagation()}>
        <button type="button" className="modal-close post-expanded-close" onClick={onClose} aria-label="Close post">
          <CloseIcon size={18} />
        </button>
        <PostExpanded post={post} />
      </div>
    </div>
  );
}

import { useCallback, useEffect, useRef, useState } from 'react';
import type { Post } from '../api/apiSlice';

interface UseFeedKeyboardNavigationOptions {
  posts: Post[] | undefined;
  enabled?: boolean;
  onOpenPost: (post: Post) => void;
  onToggleLike?: (post: Post) => void;
  onToggleRepost?: (post: Post) => void;
}

export function useFeedKeyboardNavigation({
  posts = [],
  enabled = true,
  onOpenPost,
  onToggleLike,
  onToggleRepost,
}: UseFeedKeyboardNavigationOptions) {
  const [activeIndex, setActiveIndex] = useState(-1);
  const postElementsRef = useRef<Map<number, HTMLElement>>(new Map());

  useEffect(() => {
    if (!posts || posts.length === 0) {
      setActiveIndex(-1);
    } else if (activeIndex >= posts.length) {
      setActiveIndex(posts.length - 1);
    }
  }, [posts, activeIndex]);

  const registerPostRef = useCallback((index: number, el: HTMLElement | null) => {
    if (el) postElementsRef.current.set(index, el);
    else postElementsRef.current.delete(index);
  }, []);

  const scrollToPost = useCallback((index: number) => {
    postElementsRef.current.get(index)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  }, []);

  const scrollToActivePost = useCallback(() => {
    if (activeIndex >= 0) scrollToPost(activeIndex);
  }, [activeIndex, scrollToPost]);

  useEffect(() => {
    if (!enabled || !posts || posts.length === 0) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      const target = event.target as HTMLElement | null;
      if (
        target &&
        (target.tagName === 'INPUT' ||
          target.tagName === 'TEXTAREA' ||
          target.tagName === 'SELECT' ||
          target.isContentEditable)
      ) {
        return;
      }
      if (event.metaKey || event.ctrlKey || event.altKey) return;

      const key = event.key;

      if (key === 'j' || key === 'J' || key === 'ArrowDown') {
        event.preventDefault();
        setActiveIndex((prev) => {
          const next = prev < posts.length - 1 ? prev + 1 : 0;
          scrollToPost(next);
          return next;
        });
        return;
      }

      if (key === 'k' || key === 'K' || key === 'ArrowUp') {
        event.preventDefault();
        setActiveIndex((prev) => {
          const next = prev > 0 ? prev - 1 : posts.length - 1;
          scrollToPost(next);
          return next;
        });
        return;
      }

      if (key === 'Enter') {
        if (activeIndex >= 0 && activeIndex < posts.length) {
          event.preventDefault();
          onOpenPost(posts[activeIndex]);
        }
        return;
      }

      if ((key === 'l' || key === 'L') && onToggleLike) {
        if (activeIndex >= 0 && activeIndex < posts.length) {
          event.preventDefault();
          onToggleLike(posts[activeIndex]);
        }
        return;
      }

      if ((key === 'r' || key === 'R') && onToggleRepost) {
        if (activeIndex >= 0 && activeIndex < posts.length) {
          event.preventDefault();
          onToggleRepost(posts[activeIndex]);
        }
        return;
      }

      if (key === 'Escape') {
        setActiveIndex(-1);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [enabled, posts, activeIndex, onOpenPost, onToggleLike, onToggleRepost, scrollToPost]);

  const activePostId =
    posts && activeIndex >= 0 && activeIndex < posts.length ? posts[activeIndex].postId : null;

  return { activeIndex, activePostId, setActiveIndex, registerPostRef, scrollToActivePost };
}

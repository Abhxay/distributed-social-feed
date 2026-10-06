import { useEffect, useRef, useState } from 'react';
import { useFollowMutation, useSearchUsersQuery, useUnfollowMutation } from '../../api/apiSlice';
import { useDebouncedValue } from '../../hooks/useDebouncedValue';
import { CloseIcon, SearchIcon } from '../../components/Icons';

export function FollowSearch() {
  const [query, setQuery] = useState('');
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const debouncedQuery = useDebouncedValue(query, 400);
  const { data: results = [] } = useSearchUsersQuery(debouncedQuery, {
    skip: debouncedQuery.length < 2,
  });
  const [follow] = useFollowMutation();
  const [unfollow] = useUnfollowMutation();

  // ponytail: /users/search doesn't return a follow-state flag, so we track which ids were
  // followed during this session locally. A real deployment would get `isFollowing` back from
  // the backend instead (same pattern Explore already uses properly).
  const [followedIds, setFollowedIds] = useState<Set<string>>(new Set());

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const toggleFollow = async (userId: string) => {
    if (followedIds.has(userId)) {
      await unfollow(userId);
      setFollowedIds((prev) => {
        const next = new Set(prev);
        next.delete(userId);
        return next;
      });
    } else {
      await follow(userId);
      setFollowedIds((prev) => new Set(prev).add(userId));
    }
  };

  const handleClear = () => {
    setQuery('');
    setIsOpen(false);
  };

  return (
    <div className="search-bar-section" ref={containerRef}>
      <div className="search-bar">
        <span className="search-bar-icon">
          <SearchIcon size={15} />
        </span>
        <input
          aria-label="Search users"
          placeholder="Search users…"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setIsOpen(true);
          }}
          onFocus={() => query.length >= 2 && setIsOpen(true)}
        />
        {query && (
          <button type="button" className="search-clear-btn" onClick={handleClear} aria-label="Clear search">
            <CloseIcon size={14} />
          </button>
        )}
      </div>
      {isOpen && query.length >= 2 && (
        <ul className="search-results">
          {results.map((user) => (
            <li key={user.id}>
              <span>{user.username}</span>
              <button type="button" onClick={() => toggleFollow(user.id)}>
                {followedIds.has(user.id) ? 'Unfollow' : 'Follow'}
              </button>
            </li>
          ))}
          {results.length === 0 && (
            <li className="muted search-no-results">No matching accounts found for &ldquo;{query}&rdquo;</li>
          )}
        </ul>
      )}
    </div>
  );
}

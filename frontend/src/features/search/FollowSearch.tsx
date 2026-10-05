import { useState } from 'react';
import { useFollowMutation, useSearchUsersQuery, useUnfollowMutation } from '../../api/apiSlice';
import { useDebouncedValue } from '../../hooks/useDebouncedValue';

export function FollowSearch() {
  const [query, setQuery] = useState('');
  const debouncedQuery = useDebouncedValue(query, 400);
  const { data: results = [] } = useSearchUsersQuery(debouncedQuery, {
    skip: debouncedQuery.length < 2,
  });
  const [follow] = useFollowMutation();
  const [unfollow] = useUnfollowMutation();

  // ponytail: /users/search doesn't return a follow-state flag, so we track
  // which ids were followed during this session locally. A real deployment
  // would get `isFollowing` back from the backend instead.
  const [followedIds, setFollowedIds] = useState<Set<string>>(new Set());

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

  return (
    <div className="follow-search">
      <label htmlFor="follow-search-input">Search users</label>
      <input
        id="follow-search-input"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      <ul>
        {results.map((user) => (
          <li key={user.id}>
            <span>{user.username}</span>
            <button type="button" onClick={() => toggleFollow(user.id)}>
              {followedIds.has(user.id) ? 'Unfollow' : 'Follow'}
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}

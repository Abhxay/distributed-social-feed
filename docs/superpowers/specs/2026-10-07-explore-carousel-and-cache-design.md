# Explore page: carousel pagination + batch cache

## Problem

Clicking "Explore more" hides the previous 5-user batch and replaces it, with a
noticeable wait each time — measured at 1.7–2.3s per `/users/explore` call
against the live backend, even warm (no cold start). Root cause: `getRanking()`
makes three sequential network round trips per request (Redis `ZREVRANGE`, a
Postgres username lookup, a Postgres follow-check), each paying full
cross-region latency.

## Design

### Backend: shared batch cache (cache-aside, 60s TTL)

New Redis key `explore:batch:{offset}:{limit}` → JSON array of
`{userId, username, activityScore}`.

- Hit: skip the `ZREVRANGE` and the Postgres username lookup entirely.
- Miss: run the existing path, write the result to cache before returning.
- `isFollowing` is **never** cached — it's per-viewer and must reflect a
  Follow/Unfollow click immediately, so it's computed live on every request
  regardless of batch cache state.
- No stampede lock (unlike the feed cache's lease pattern): at this app's
  traffic, two concurrent misses on the same offset is vanishingly unlikely,
  and the cost of that collision is one redundant rebuild, not a thundering
  herd. Deliberate simplification, not an oversight.
- Redis failures aren't specially handled, matching the rest of this app's
  cache-aside code (nothing else wraps Redis calls in try/catch either).

### Frontend: prefetch ahead

As soon as a batch renders with `hasMore`, eagerly prefetch the next offset
(RTK Query's `usePrefetch`) so the data is usually already cached by the time
the user clicks "Explore more" — masked by the time it takes to read 5 names.

### Frontend: carousel (side-peek, option B)

- Track `offset` (current) and `previousOffset` (null unless the last move was
  forward).
- Current batch renders normally. If `previousOffset` is set, that batch
  (already in RTK Query cache — no extra network call) renders blurred,
  scaled down, offset to the left.
- The blurred peek is clickable and triggers the same "go back" action as the
  existing `‹` arrow (kept for keyboard/accessibility).
- Going back clears `previousOffset` — no peek when navigating backward, only
  forward. Confirmed explicitly: asymmetric by design, not a bug.

## Testing

- Backend: `ExploreService`/`ExploreController` test covering cache hit (skips
  Redis ranking + Postgres username lookup), cache miss (populates cache),
  and `isFollowing` always computed live even on a cache hit.
- Frontend: one test protecting the one real behavioral rule — peek shows
  going forward, not going backward.

## Explicitly out of scope

- Caching `isFollowing` or any other per-viewer data — breaks live
  Follow/Unfollow feedback.
- A stampede lock/lease for the batch cache — unjustified complexity at this
  traffic scale.
- Merging the two Postgres queries (username + follow-check) into one join —
  subsumed by the cache; only matters on a cache miss, which is now rare and
  shared across all viewers, not worth the extra complexity on top of caching.

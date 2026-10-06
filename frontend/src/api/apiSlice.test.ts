import { waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createTestStore } from '../test-utils/renderWithProviders';
import { apiSlice, type Post } from './apiSlice';

describe('apiSlice optimistic like update', () => {
  beforeEach(() => {
    // A promise that never resolves — lets us inspect the optimistic cache
    // patch before any network response could possibly land.
    vi.stubGlobal(
      'fetch',
      vi.fn(() => new Promise(() => {})),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('flips likedByMe and increments likeCount before the request resolves', async () => {
    const store = createTestStore();
    const fakePost: Post = {
      postId: 'post-1',
      authorId: 'user-1',
      authorUsername: 'alice',
      headline: 'Hello',
      body: 'hello world',
      imageUrl: null,
      likeCount: 0,
      likedByMe: false,
      commentCount: 0,
      repostCount: 0,
      repostedByMe: false,
      createdAt: new Date().toISOString(),
    };

    await store.dispatch(apiSlice.util.upsertQueryData('getFeed', undefined, [fakePost]));
    store.dispatch(apiSlice.endpoints.like.initiate('post-1'));

    // Wait only for the synchronous optimistic patch to land in the cache —
    // NOT for the network call, which (per the mock above) never resolves.
    await waitFor(() => {
      const cached = apiSlice.endpoints.getFeed.select(undefined)(store.getState()).data;
      expect(cached?.[0].likedByMe).toBe(true);
    });

    const cached = apiSlice.endpoints.getFeed.select(undefined)(store.getState()).data;
    expect(cached?.[0].likeCount).toBe(1);
  });
});

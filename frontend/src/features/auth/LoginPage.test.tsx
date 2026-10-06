import { fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiSlice } from '../../api/apiSlice';
import { renderWithProviders } from '../../test-utils/renderWithProviders';
import { LoginPage } from './LoginPage';

describe('LoginPage', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn(() =>
        Promise.resolve(
          new Response(JSON.stringify({ accessToken: 'at', refreshToken: 'rt' }), { status: 200 }),
        ),
      ),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('resets the RTK Query cache on login so a previous user\'s cached data cannot leak in', async () => {
    const { store } = renderWithProviders(<LoginPage />);

    // Seed the cache as if a previous user's (empty) feed was already fetched in this tab.
    await store.dispatch(apiSlice.util.upsertQueryData('getFeed', undefined, []));
    expect(apiSlice.endpoints.getFeed.select(undefined)(store.getState()).data).toEqual([]);

    fireEvent.change(screen.getByLabelText(/username/i), { target: { value: 'maya_k' } });
    fireEvent.change(screen.getByLabelText(/password/i), { target: { value: 'pw' } });
    fireEvent.click(screen.getByRole('button', { name: /log in/i }));

    await waitFor(() => {
      // After login, the stale cache entry must be gone (undefined), not the old empty array —
      // this is what forces a fresh fetch for the newly logged-in user instead of showing stale data.
      expect(apiSlice.endpoints.getFeed.select(undefined)(store.getState()).data).toBeUndefined();
    });
  });
});

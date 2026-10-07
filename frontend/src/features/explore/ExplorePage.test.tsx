import { fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '../../test-utils/renderWithProviders';
import { ExplorePage } from './ExplorePage';

function batch(prefix: string, hasMore: boolean) {
  return {
    users: Array.from({ length: 5 }, (_, i) => ({
      id: `${prefix}${i}`,
      username: `${prefix}_user_${i}`,
      activityScore: 10 - i,
      isFollowing: false,
    })),
    totalRanked: 10,
    hasMore,
  };
}

describe('ExplorePage', () => {
  beforeEach(() => {
    vi.stubGlobal(
      'fetch',
      vi.fn((request: Request) => {
        const url = new URL(request.url);
        if (url.pathname === '/users/explore') {
          const offset = url.searchParams.get('offset');
          const body = offset === '5' ? batch('b', false) : batch('a', true);
          return Promise.resolve(new Response(JSON.stringify(body), { status: 200 }));
        }
        return Promise.resolve(new Response('null', { status: 200 }));
      }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('shows a blurred peek of the batch you came from after moving forward, but not after moving back', async () => {
    const { container } = renderWithProviders(<ExplorePage />);

    await screen.findByText('a_user_0');
    expect(container.querySelector('.explore-list-peek')).toBeNull();

    fireEvent.click(screen.getByRole('button', { name: /explore more/i }));

    await screen.findByText('b_user_0');
    expect(container.querySelector('.explore-list-peek')).not.toBeNull();

    fireEvent.click(screen.getByRole('button', { name: /previous/i }));

    await waitFor(() => {
      expect(container.querySelector('.explore-list-peek')).toBeNull();
    });
  });
});

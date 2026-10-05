import { fireEvent, screen, waitFor } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '../../test-utils/renderWithProviders';
import { FeedPage } from './FeedPage';

describe('FeedPage', () => {
  let feedRequestCount: number;

  beforeEach(() => {
    feedRequestCount = 0;
    vi.stubGlobal(
      'fetch',
      vi.fn((request: Request) => {
        if (request.url.includes('/feed')) {
          feedRequestCount += 1;
          // First load: empty feed. After the post is created and the
          // 'Feed' tag is invalidated, the refetch reflects the new post.
          const posts =
            feedRequestCount === 1
              ? []
              : [
                  {
                    postId: 'post-1',
                    authorId: 'me',
                    authorUsername: 'tester',
                    body: 'hello from the test',
                    likeCount: 0,
                    likedByMe: false,
                    createdAt: new Date().toISOString(),
                  },
                ];
          return Promise.resolve(new Response(JSON.stringify(posts), { status: 200 }));
        }
        if (request.url.includes('/posts') && request.method === 'POST') {
          return Promise.resolve(
            new Response(JSON.stringify({ postId: 'post-1' }), { status: 200 }),
          );
        }
        return Promise.resolve(new Response('null', { status: 200 }));
      }),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('calls createPost on submit and shows the new post once the feed reflects it', async () => {
    renderWithProviders(<FeedPage />);

    const textarea = await screen.findByLabelText(/new post/i);
    fireEvent.change(textarea, { target: { value: 'hello from the test' } });
    fireEvent.click(screen.getByRole('button', { name: /^post$/i }));

    await waitFor(() => {
      const postCall = (fetch as ReturnType<typeof vi.fn>).mock.calls.find((call) => {
        const request = call[0] as Request;
        return request.url.includes('/posts') && request.method === 'POST';
      });
      expect(postCall).toBeDefined();
    });

    await waitFor(() => {
      expect(screen.getByText('hello from the test')).toBeInTheDocument();
    });
  });
});

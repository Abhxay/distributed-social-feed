import { act, fireEvent, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '../../test-utils/renderWithProviders';
import { SignupPage } from './SignupPage';

describe('SignupPage', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.stubGlobal(
      'fetch',
      vi.fn((request: Request) => {
        if (request.url.includes('/auth/check-username')) {
          return Promise.resolve(
            new Response(JSON.stringify({ available: true }), { status: 200 }),
          );
        }
        return Promise.resolve(new Response('null', { status: 200 }));
      }),
    );
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('does not fire check-username calls until typing pauses', async () => {
    renderWithProviders(<SignupPage />);
    const input = screen.getByLabelText(/username/i);

    // Three rapid keystrokes, none separated by the full debounce window.
    fireEvent.change(input, { target: { value: 'a' } });
    fireEvent.change(input, { target: { value: 'ab' } });
    fireEvent.change(input, { target: { value: 'abc' } });

    // Still within the debounce window — no network call yet.
    act(() => {
      vi.advanceTimersByTime(200);
    });
    expect(fetch).not.toHaveBeenCalled();

    // The pause elapses fully — exactly one call fires, for the final value.
    act(() => {
      vi.advanceTimersByTime(400);
    });
    // The query dispatch that follows the debounced state update resolves
    // on a microtask, one tick after the timer itself fires.
    await Promise.resolve();
    expect(fetch).toHaveBeenCalledTimes(1);
    const calledUrl = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][0].url as string;
    expect(calledUrl).toContain('username=abc');
  });
});

import { act, fireEvent, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '../../test-utils/renderWithProviders';
import { FollowSearch } from './FollowSearch';

describe('FollowSearch', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.resolve(new Response(JSON.stringify([]), { status: 200 }))),
    );
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  it('does not fire search calls until typing pauses', async () => {
    renderWithProviders(<FollowSearch />);
    const input = screen.getByLabelText(/search users/i);

    fireEvent.change(input, { target: { value: 'a' } });
    fireEvent.change(input, { target: { value: 'ab' } });
    fireEvent.change(input, { target: { value: 'abc' } });

    act(() => {
      vi.advanceTimersByTime(200);
    });
    expect(fetch).not.toHaveBeenCalled();

    act(() => {
      vi.advanceTimersByTime(400);
    });
    // The query dispatch that follows the debounced state update resolves
    // on a microtask, one tick after the timer itself fires.
    await Promise.resolve();
    expect(fetch).toHaveBeenCalledTimes(1);
    const calledUrl = (fetch as ReturnType<typeof vi.fn>).mock.calls[0][0].url as string;
    expect(calledUrl).toContain('q=abc');
  });
});

import { act, renderHook } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { useDebouncedValue } from './useDebouncedValue';

describe('useDebouncedValue', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('only reflects the last of several rapid changes once the delay elapses', () => {
    const { result, rerender } = renderHook(({ value }) => useDebouncedValue(value, 400), {
      initialProps: { value: 'a' },
    });

    expect(result.current).toBe('a');

    // Three rapid changes, each inside the previous change's debounce window —
    // none of the intermediate values should ever be committed.
    rerender({ value: 'ab' });
    act(() => {
      vi.advanceTimersByTime(100);
    });
    rerender({ value: 'abc' });
    act(() => {
      vi.advanceTimersByTime(100);
    });
    rerender({ value: 'abcd' });

    // Still inside the debounce window started by the last change.
    act(() => {
      vi.advanceTimersByTime(399);
    });
    expect(result.current).toBe('a');

    // The window elapses — only the final value lands.
    act(() => {
      vi.advanceTimersByTime(1);
    });
    expect(result.current).toBe('abcd');
  });
});

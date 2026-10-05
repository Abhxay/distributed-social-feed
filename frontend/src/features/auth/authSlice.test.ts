import { describe, expect, it } from 'vitest';
import { authSlice, logout, setTokens } from './authSlice';

describe('authSlice', () => {
  it('clears the access token back to null after setTokens then logout', () => {
    let state = authSlice.reducer(undefined, { type: 'init' });
    expect(state.accessToken).toBeNull();

    state = authSlice.reducer(state, setTokens({ accessToken: 'abc', refreshToken: 'def' }));
    expect(state.accessToken).toBe('abc');
    expect(state.refreshToken).toBe('def');

    state = authSlice.reducer(state, logout());
    expect(state.accessToken).toBeNull();
    expect(state.refreshToken).toBeNull();
  });
});

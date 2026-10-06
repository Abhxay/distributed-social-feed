import { createSlice, type PayloadAction } from '@reduxjs/toolkit';

interface AuthState {
  accessToken: string | null;
  refreshToken: string | null;
}

// Without this, a plain browser refresh (F5) loses the session entirely — Redux state lives only
// in memory, so the user would get bounced to /login every time they reload the page.
function loadInitialState(): AuthState {
  try {
    return {
      accessToken: localStorage.getItem('accessToken'),
      refreshToken: localStorage.getItem('refreshToken'),
    };
  } catch {
    return { accessToken: null, refreshToken: null };
  }
}

const initialState: AuthState = loadInitialState();

export const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setTokens: (state, action: PayloadAction<{ accessToken: string; refreshToken: string }>) => {
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
      try {
        localStorage.setItem('accessToken', action.payload.accessToken);
        localStorage.setItem('refreshToken', action.payload.refreshToken);
      } catch {
        // per-viewer convenience only — session still works for this tab even if it can't persist
      }
    },
    logout: (state) => {
      state.accessToken = null;
      state.refreshToken = null;
      try {
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
      } catch {
        // ignore
      }
    },
  },
});

export const { setTokens, logout } = authSlice.actions;
export const selectAccessToken = (state: { auth: AuthState }) => state.auth.accessToken;

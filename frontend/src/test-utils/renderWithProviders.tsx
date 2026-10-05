import type { ReactElement } from 'react';
import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { apiSlice } from '../api/apiSlice';
import { authSlice } from '../features/auth/authSlice';

export function createTestStore() {
  return configureStore({
    reducer: {
      auth: authSlice.reducer,
      [apiSlice.reducerPath]: apiSlice.reducer,
    },
    middleware: (getDefaultMiddleware) => getDefaultMiddleware().concat(apiSlice.middleware),
  });
}

export function renderWithProviders(
  ui: ReactElement,
  store = createTestStore(),
  initialEntries: string[] = ['/'],
) {
  return {
    store,
    ...render(
      <Provider store={store}>
        <MemoryRouter initialEntries={initialEntries}>{ui}</MemoryRouter>
      </Provider>,
    ),
  };
}

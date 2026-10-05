import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { RootState } from '../app/store';

export interface FeedPost {
  postId: string;
  authorId: string;
  authorUsername: string;
  body: string;
  likeCount: number;
  likedByMe: boolean;
  createdAt: string;
}

export interface UserSummary {
  id: string;
  username: string;
}

interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export const apiSlice = createApi({
  reducerPath: 'api',
  baseQuery: fetchBaseQuery({
    // Falls back to an absolute default: the Fetch/Request spec requires an
    // absolute URL outside a browser document context (e.g. under Node in
    // tests), so a relative/empty baseUrl breaks there even though it would
    // resolve fine against window.location in an actual browser.
    baseUrl: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    prepareHeaders: (headers, { getState }) => {
      const token = (getState() as RootState).auth.accessToken;
      if (token) headers.set('Authorization', `Bearer ${token}`);
      return headers;
    },
  }),
  tagTypes: ['Feed'],
  endpoints: (builder) => ({
    signup: builder.mutation<{ userId: string }, { username: string; password: string }>({
      query: (body) => ({ url: '/auth/signup', method: 'POST', body }),
    }),

    checkUsername: builder.query<{ available: boolean }, string>({
      query: (username) => `/auth/check-username?username=${encodeURIComponent(username)}`,
    }),

    login: builder.mutation<AuthTokens, { username: string; password: string }>({
      query: (body) => ({ url: '/auth/login', method: 'POST', body }),
    }),

    refresh: builder.mutation<AuthTokens, { refreshToken: string }>({
      query: (body) => ({ url: '/auth/refresh', method: 'POST', body }),
    }),

    searchUsers: builder.query<UserSummary[], string>({
      query: (q) => `/users/search?q=${encodeURIComponent(q)}`,
    }),

    follow: builder.mutation<void, string>({
      query: (userId) => ({ url: `/users/${userId}/follow`, method: 'POST' }),
    }),

    unfollow: builder.mutation<void, string>({
      query: (userId) => ({ url: `/users/${userId}/follow`, method: 'DELETE' }),
    }),

    createPost: builder.mutation<{ postId: string }, string>({
      query: (body) => ({
        url: '/posts',
        method: 'POST',
        body: { body },
        // ponytail: a production version would need to reuse the same
        // Idempotency-Key across RTK-Query-internal retries of one logical
        // attempt, not regenerate a fresh key per retry. Out of scope here —
        // this showcase generates one key per mutation call.
        headers: { 'Idempotency-Key': crypto.randomUUID() },
      }),
      invalidatesTags: ['Feed'],
    }),

    getFeed: builder.query<FeedPost[], void>({
      query: () => '/feed',
      providesTags: ['Feed'],
    }),

    like: builder.mutation<{ liked: true }, string>({
      query: (postId) => ({
        url: `/posts/${postId}/likes`,
        method: 'POST',
        headers: { 'Idempotency-Key': crypto.randomUUID() },
      }),
      async onQueryStarted(postId, { dispatch, queryFulfilled }) {
        const patchResult = dispatch(
          apiSlice.util.updateQueryData('getFeed', undefined, (draft) => {
            const post = draft.find((p) => p.postId === postId);
            if (post) {
              post.likedByMe = true;
              post.likeCount += 1;
            }
          }),
        );
        try {
          await queryFulfilled;
        } catch {
          patchResult.undo();
        }
      },
    }),

    unlike: builder.mutation<{ liked: false }, string>({
      query: (postId) => ({ url: `/posts/${postId}/likes`, method: 'DELETE' }),
      async onQueryStarted(postId, { dispatch, queryFulfilled }) {
        const patchResult = dispatch(
          apiSlice.util.updateQueryData('getFeed', undefined, (draft) => {
            const post = draft.find((p) => p.postId === postId);
            if (post) {
              post.likedByMe = false;
              post.likeCount -= 1;
            }
          }),
        );
        try {
          await queryFulfilled;
        } catch {
          patchResult.undo();
        }
      },
    }),
  }),
});

export const {
  useSignupMutation,
  useCheckUsernameQuery,
  useLoginMutation,
  useRefreshMutation,
  useSearchUsersQuery,
  useFollowMutation,
  useUnfollowMutation,
  useCreatePostMutation,
  useGetFeedQuery,
  useLikeMutation,
  useUnlikeMutation,
} = apiSlice;

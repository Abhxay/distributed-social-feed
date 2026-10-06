import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { RootState } from '../app/store';

export interface Post {
  postId: string;
  authorId: string;
  authorUsername: string;
  headline: string;
  body: string;
  imageUrl: string | null;
  likeCount: number;
  likedByMe: boolean;
  commentCount: number;
  repostCount: number;
  repostedByMe: boolean;
  createdAt: string;
}

export interface Comment {
  id: string;
  postId: string;
  authorId: string;
  authorUsername: string;
  body: string;
  createdAt: string;
}

export interface Profile {
  id: string;
  username: string;
  postCount: number;
  likesReceived: number;
  commentsReceived: number;
  activityScore: number;
  posts: Post[];
  reposts: Post[];
}

export interface ExplorePage {
  users: ExploreUser[];
  totalRanked: number;
  hasMore: boolean;
}

export interface UserSummary {
  id: string;
  username: string;
}

export interface ExploreUser {
  id: string;
  username: string;
  activityScore: number;
  isFollowing: boolean;
}

interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

// Shared by like/unlike/repost/unrepost: the same post can be sitting in the feed
// list, the single-post cache, and the profile's posts/reposts lists at once, so
// one optimistic toggle needs to patch all three. updateQueryData no-ops cleanly
// on a cache entry that isn't populated, so this is safe to call unconditionally.
function patchPostEverywhere(postId: string, mutate: (post: Post) => void) {
  return [
    apiSlice.util.updateQueryData('getFeed', undefined, (draft) => {
      const post = draft.find((p) => p.postId === postId);
      if (post) mutate(post);
    }),
    apiSlice.util.updateQueryData('getPost', postId, mutate),
    apiSlice.util.updateQueryData('getProfile', undefined, (draft) => {
      const post = draft.posts.find((p) => p.postId === postId) ?? draft.reposts.find((p) => p.postId === postId);
      if (post) mutate(post);
    }),
  ];
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
  tagTypes: ['Feed', 'Explore', 'Comments', 'Profile'],
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

    exploreUsers: builder.query<ExplorePage, { offset: number; limit: number }>({
      query: ({ offset, limit }) => `/users/explore?offset=${offset}&limit=${limit}`,
      providesTags: ['Explore'],
    }),

    getProfile: builder.query<Profile, void>({
      query: () => '/users/me',
      providesTags: ['Profile'],
    }),

    changePassword: builder.mutation<{ success: boolean }, { currentPassword: string; newPassword: string }>({
      query: (body) => ({ url: '/auth/change-password', method: 'POST', body }),
    }),

    follow: builder.mutation<void, string>({
      query: (userId) => ({ url: `/users/${userId}/follow`, method: 'POST' }),
      invalidatesTags: ['Explore'],
    }),

    unfollow: builder.mutation<void, string>({
      query: (userId) => ({ url: `/users/${userId}/follow`, method: 'DELETE' }),
      invalidatesTags: ['Explore'],
    }),

    createPost: builder.mutation<{ postId: string }, { headline: string; body: string; imageUrl?: string }>({
      query: (body) => ({
        url: '/posts',
        method: 'POST',
        body,
        // ponytail: a production version would need to reuse the same
        // Idempotency-Key across RTK-Query-internal retries of one logical
        // attempt, not regenerate a fresh key per retry. Out of scope here —
        // this showcase generates one key per mutation call.
        headers: { 'Idempotency-Key': crypto.randomUUID() },
      }),
      invalidatesTags: ['Feed', 'Profile'],
    }),

    uploadPostImage: builder.mutation<Post, { postId: string; file: File }>({
      query: ({ postId, file }) => {
        const formData = new FormData();
        formData.append('file', file);
        // fetchBaseQuery only JSON-encodes plain objects/arrays; a FormData body is passed
        // through as-is and its content-type header is left for the browser to set (with
        // the multipart boundary), so no header overrides are needed here.
        return { url: `/posts/${postId}/image`, method: 'POST', body: formData };
      },
      invalidatesTags: ['Feed', 'Profile'],
    }),

    getFeed: builder.query<Post[], void>({
      query: () => '/feed',
      providesTags: ['Feed'],
    }),

    getPost: builder.query<Post, string>({
      query: (postId) => `/posts/${postId}`,
    }),

    like: builder.mutation<{ liked: true }, string>({
      query: (postId) => ({
        url: `/posts/${postId}/likes`,
        method: 'POST',
        headers: { 'Idempotency-Key': crypto.randomUUID() },
      }),
      async onQueryStarted(postId, { dispatch, queryFulfilled }) {
        const [feed, post, profile] = patchPostEverywhere(postId, (post) => {
          post.likedByMe = true;
          post.likeCount += 1;
        });
        const patches = [dispatch(feed), dispatch(post), dispatch(profile)];
        try {
          await queryFulfilled;
        } catch {
          patches.forEach((p) => p.undo());
        }
      },
    }),

    getComments: builder.query<Comment[], string>({
      query: (postId) => `/posts/${postId}/comments`,
      providesTags: (_result, _error, postId) => [{ type: 'Comments', id: postId }],
    }),

    createComment: builder.mutation<Comment, { postId: string; body: string }>({
      query: ({ postId, body }) => ({ url: `/posts/${postId}/comments`, method: 'POST', body: { body } }),
      invalidatesTags: (_result, _error, { postId }) => [{ type: 'Comments', id: postId }],
    }),

    unlike: builder.mutation<{ liked: false }, string>({
      query: (postId) => ({ url: `/posts/${postId}/likes`, method: 'DELETE' }),
      async onQueryStarted(postId, { dispatch, queryFulfilled }) {
        const [feed, post, profile] = patchPostEverywhere(postId, (post) => {
          post.likedByMe = false;
          post.likeCount = Math.max(0, post.likeCount - 1); // never show negative, even transiently
        });
        const patches = [dispatch(feed), dispatch(post), dispatch(profile)];
        try {
          await queryFulfilled;
        } catch {
          patches.forEach((p) => p.undo());
        }
      },
    }),

    repostPost: builder.mutation<{ reposted: true }, string>({
      query: (postId) => ({
        url: `/posts/${postId}/reposts`,
        method: 'POST',
        headers: { 'Idempotency-Key': crypto.randomUUID() },
      }),
      async onQueryStarted(postId, { dispatch, queryFulfilled }) {
        const [feed, post, profile] = patchPostEverywhere(postId, (post) => {
          post.repostedByMe = true;
          post.repostCount += 1;
        });
        const patches = [dispatch(feed), dispatch(post), dispatch(profile)];
        try {
          await queryFulfilled;
        } catch {
          patches.forEach((p) => p.undo());
        }
      },
    }),

    unrepostPost: builder.mutation<{ reposted: false }, string>({
      query: (postId) => ({ url: `/posts/${postId}/reposts`, method: 'DELETE' }),
      async onQueryStarted(postId, { dispatch, queryFulfilled }) {
        const [feed, post, profile] = patchPostEverywhere(postId, (post) => {
          post.repostedByMe = false;
          post.repostCount = Math.max(0, post.repostCount - 1);
        });
        const patches = [dispatch(feed), dispatch(post), dispatch(profile)];
        try {
          await queryFulfilled;
        } catch {
          patches.forEach((p) => p.undo());
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
  useExploreUsersQuery,
  useFollowMutation,
  useUnfollowMutation,
  useGetCommentsQuery,
  useCreateCommentMutation,
  useGetProfileQuery,
  useChangePasswordMutation,
  useCreatePostMutation,
  useUploadPostImageMutation,
  useGetFeedQuery,
  useGetPostQuery,
  useLikeMutation,
  useUnlikeMutation,
  useRepostPostMutation,
  useUnrepostPostMutation,
} = apiSlice;

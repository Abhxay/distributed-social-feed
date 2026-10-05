import { useState } from 'react';
import type { FormEvent } from 'react';
import { useCreatePostMutation } from '../../api/apiSlice';

export function PostComposer() {
  const [body, setBody] = useState('');
  const [createPost, { isLoading }] = useCreatePostMutation();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!body.trim()) return;
    await createPost(body);
    setBody('');
  };

  return (
    <form onSubmit={handleSubmit} className="post-composer">
      <label htmlFor="post-body">New post</label>
      <textarea id="post-body" value={body} onChange={(e) => setBody(e.target.value)} />
      <button type="submit" disabled={isLoading || !body.trim()}>
        Post
      </button>
    </form>
  );
}

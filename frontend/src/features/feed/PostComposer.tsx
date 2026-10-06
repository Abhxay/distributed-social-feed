import { useState } from 'react';
import type { FormEvent } from 'react';
import { useCreatePostMutation } from '../../api/apiSlice';

export function PostComposer() {
  const [headline, setHeadline] = useState('');
  const [body, setBody] = useState('');
  const [createPost, { isLoading }] = useCreatePostMutation();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!headline.trim() || !body.trim()) return;
    await createPost({ headline, body });
    setHeadline('');
    setBody('');
  };

  return (
    <form onSubmit={handleSubmit} className="post-composer">
      <label htmlFor="post-headline">Headline</label>
      <input
        id="post-headline"
        maxLength={150}
        placeholder="Give it a headline"
        value={headline}
        onChange={(e) => setHeadline(e.target.value)}
      />
      <span className="muted composer-counter">{headline.length}/150</span>

      <label htmlFor="post-body">New post</label>
      <textarea
        id="post-body"
        rows={6}
        maxLength={2000}
        placeholder="Share something — a thought, a story, a whole paragraph if you want."
        value={body}
        onChange={(e) => setBody(e.target.value)}
      />
      <span className="muted composer-counter">{body.length}/2000</span>
      <button type="submit" disabled={isLoading || !headline.trim() || !body.trim()}>
        Post
      </button>
    </form>
  );
}

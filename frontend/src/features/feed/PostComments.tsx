import { useState } from 'react';
import type { FormEvent } from 'react';
import { useCreateCommentMutation, useGetCommentsQuery } from '../../api/apiSlice';

function formatDate(iso: string) {
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

export function PostComments({ postId }: { postId: string }) {
  const [body, setBody] = useState('');
  const { data: comments = [] } = useGetCommentsQuery(postId);
  const [createComment, { isLoading }] = useCreateCommentMutation();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!body.trim()) return;
    await createComment({ postId, body: body.trim() });
    setBody('');
  };

  return (
    <div className="discussion-panel">
      <h3 className="discussion-heading">Discussion ({comments.length})</h3>

      <form onSubmit={handleSubmit} className="discussion-composer">
        <div className="comment-avatar" aria-hidden="true" />
        <div className="discussion-composer-fields">
          <textarea
            rows={2}
            aria-label="Add a comment"
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="Add a substantive response or feedback…"
          />
          <div className="discussion-composer-actions">
            <button type="submit" disabled={isLoading || !body.trim()}>
              {isLoading ? 'Posting…' : 'Comment'}
            </button>
          </div>
        </div>
      </form>

      {comments.length === 0 ? (
        <p className="muted discussion-empty">No comments on this post yet.</p>
      ) : (
        <ul className="comments-list">
          {comments.map((c) => (
            <li key={c.id} className="comment-row">
              <div className="comment-row-header">
                <div className="comment-avatar" aria-hidden="true" />
                <span className="comment-author">{c.authorUsername}</span>
                <span className="muted comment-date tabular-nums">{formatDate(c.createdAt)}</span>
              </div>
              <p className="comment-body">{c.body}</p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

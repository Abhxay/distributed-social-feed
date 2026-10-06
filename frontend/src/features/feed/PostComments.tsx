import { useState } from 'react';
import type { FormEvent } from 'react';
import { useCreateCommentMutation, useGetCommentsQuery } from '../../api/apiSlice';

export function PostComments({ postId }: { postId: string }) {
  const [expanded, setExpanded] = useState(false);
  const [body, setBody] = useState('');
  const { data: comments = [] } = useGetCommentsQuery(postId, { skip: !expanded });
  const [createComment, { isLoading }] = useCreateCommentMutation();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!body.trim()) return;
    await createComment({ postId, body });
    setBody('');
  };

  return (
    <div className="post-comments">
      <button type="button" className="comments-toggle" onClick={() => setExpanded((v) => !v)}>
        {expanded ? 'Hide comments' : 'Comments'}
      </button>

      {expanded && (
        <div className="comments-panel">
          <ul className="comments-list">
            {comments.map((c) => (
              <li key={c.id} className="comment-row">
                <span className="comment-author">{c.authorUsername}</span>
                <span className="comment-body">{c.body}</span>
              </li>
            ))}
            {comments.length === 0 && <li className="muted">No comments yet.</li>}
          </ul>

          <form onSubmit={handleSubmit} className="comment-form">
            <input
              aria-label="Add a comment"
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder="Add a comment…"
            />
            <button type="submit" disabled={isLoading || !body.trim()}>
              Reply
            </button>
          </form>
        </div>
      )}
    </div>
  );
}

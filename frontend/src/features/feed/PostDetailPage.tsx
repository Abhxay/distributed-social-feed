import { Link, useParams } from 'react-router-dom';
import { useGetPostQuery } from '../../api/apiSlice';
import { Header } from '../../components/Header';
import { ChevronLeftIcon } from '../../components/Icons';
import { PostExpanded } from './PostExpanded';

export function PostDetailPage() {
  const { postId } = useParams();
  const { data: post, isLoading } = useGetPostQuery(postId ?? '', { skip: !postId });

  return (
    <div className="feed-page">
      <Header />
      <main className="feed-main">
        <Link to="/feed" className="back-link">
          <ChevronLeftIcon size={16} />
          <span>Back to feed</span>
        </Link>

        {isLoading && <p className="muted">Loading…</p>}
        {!isLoading && !post && <p className="muted">Post not found.</p>}
        {post && <PostExpanded post={post} />}
      </main>
    </div>
  );
}

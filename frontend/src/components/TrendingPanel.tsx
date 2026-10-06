import { TrendingIcon } from './Icons';

const TOPICS = [
  { name: 'Distributed Consensus', tag: 'Indexed tag · Raft & Paxos updates' },
  { name: 'Memory-Mapped I/O', tag: 'Indexed tag · Storage engines' },
  { name: 'Speculative Decoding', tag: 'Indexed tag · Inference throughput' },
  { name: 'Zero-Knowledge Verifiers', tag: 'Indexed tag · Proof generation' },
];

export function TrendingPanel() {
  return (
    <aside className="feed-sidebar trending-panel">
      <h2>
        <TrendingIcon size={16} />
        <span>Trending</span>
      </h2>
      <p className="muted">
        Reserved for automated real-time topic indexing and clustering. Coming soon.
      </p>
      <div className="trending-list">
        {TOPICS.map((topic) => (
          <div key={topic.name} className="trending-item">
            <div className="trending-item-name">{topic.name}</div>
            <div className="muted trending-item-tag">{topic.tag}</div>
          </div>
        ))}
      </div>
    </aside>
  );
}

import { FeedPage } from './features/feed/FeedPage';

// ponytail: no routing — this is a single-page showcase of the feed. A real
// deployment would add react-router with separate routes for signup/login
// vs. the feed instead of always rendering FeedPage.
function App() {
  return <FeedPage />;
}

export default App;

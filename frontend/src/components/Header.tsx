import { NavLink } from 'react-router-dom';
import { ProfileMenu } from './ProfileMenu';

export function Header() {
  return (
    <header className="app-header">
      <span className="app-brand">distributed-social-feed</span>
      <nav className="app-nav">
        <NavLink to="/feed" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
          Feed
        </NavLink>
        <NavLink to="/explore" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
          Explore
        </NavLink>
        <ProfileMenu />
      </nav>
    </header>
  );
}

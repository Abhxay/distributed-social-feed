import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { AccountSettingsModal } from './AccountSettingsModal';
import { ProfileMenu } from './ProfileMenu';

export function Header() {
  const [settingsOpen, setSettingsOpen] = useState(false);

  return (
    <header className="app-header">
      <div className="app-brand">
        <span className="app-logo-badge">INAS</span>
        <span className="app-wordmark">INAS</span>
      </div>
      <nav className="app-nav">
        <NavLink to="/feed" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
          Feed
        </NavLink>
        <NavLink to="/explore" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
          Explore
        </NavLink>
        <ProfileMenu onOpenSettings={() => setSettingsOpen(true)} />
      </nav>
      <AccountSettingsModal open={settingsOpen} onClose={() => setSettingsOpen(false)} />
    </header>
  );
}

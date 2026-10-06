import { useEffect, useState } from 'react';
import { useDispatch } from 'react-redux';
import { Link, useNavigate } from 'react-router-dom';
import { apiSlice } from '../api/apiSlice';
import { logout } from '../features/auth/authSlice';
import { useTheme } from '../hooks/useTheme';
import { AvatarCircleIcon, LockIcon, LogoutIcon, MoonIcon, SunIcon } from './Icons';

export function ProfileMenu({ onOpenSettings }: { onOpenSettings: () => void }) {
  const [open, setOpen] = useState(false);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { theme, toggle } = useTheme();

  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOpen(false);
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open]);

  const handleLogout = () => {
    dispatch(logout());
    dispatch(apiSlice.util.resetApiState());
    navigate('/login');
  };

  return (
    <div className="profile-menu">
      <button
        type="button"
        className="profile-menu-trigger"
        onClick={() => setOpen((v) => !v)}
        aria-label="Profile menu"
        aria-expanded={open}
      >
        <AvatarCircleIcon size={18} />
      </button>
      {open && (
        <>
          <div className="profile-menu-backdrop" onClick={() => setOpen(false)} />
          <div className="profile-menu-panel">
            <Link to="/profile" className="profile-menu-item" onClick={() => setOpen(false)}>
              <AvatarCircleIcon size={15} />
              <span>Home</span>
            </Link>
            <button type="button" className="profile-menu-item" onClick={toggle}>
              {theme === 'dark' ? <SunIcon size={15} /> : <MoonIcon size={15} />}
              <span>{theme === 'dark' ? 'Light mode' : 'Dark mode'}</span>
            </button>
            <button
              type="button"
              className="profile-menu-item"
              onClick={() => {
                setOpen(false);
                onOpenSettings();
              }}
            >
              <LockIcon size={15} />
              <span>Account settings</span>
            </button>
            <div className="profile-menu-divider" />
            <button type="button" className="profile-menu-item profile-menu-logout" onClick={handleLogout}>
              <LogoutIcon size={15} />
              <span>Log out</span>
            </button>
          </div>
        </>
      )}
    </div>
  );
}

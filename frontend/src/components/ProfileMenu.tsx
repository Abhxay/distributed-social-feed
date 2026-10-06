import { useState } from 'react';
import { useDispatch } from 'react-redux';
import { Link, useNavigate } from 'react-router-dom';
import { apiSlice } from '../api/apiSlice';
import { logout } from '../features/auth/authSlice';
import { useTheme } from '../hooks/useTheme';

export function ProfileMenu() {
  const [open, setOpen] = useState(false);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { theme, toggle } = useTheme();

  const handleLogout = () => {
    dispatch(logout());
    dispatch(apiSlice.util.resetApiState());
    navigate('/login');
  };

  return (
    <div className="profile-menu">
      <button type="button" className="profile-menu-trigger" onClick={() => setOpen((v) => !v)}>
        ⚙
      </button>
      {open && (
        <>
          <div className="profile-menu-backdrop" onClick={() => setOpen(false)} />
          <div className="profile-menu-panel">
            <Link to="/profile" className="profile-menu-item" onClick={() => setOpen(false)}>
              Home
            </Link>
            <button type="button" className="profile-menu-item" onClick={toggle}>
              {theme === 'dark' ? 'Light mode' : 'Dark mode'}
            </button>
            <Link to="/settings" className="profile-menu-item" onClick={() => setOpen(false)}>
              Account settings
            </Link>
            <button type="button" className="profile-menu-item profile-menu-logout" onClick={handleLogout}>
              Log out
            </button>
          </div>
        </>
      )}
    </div>
  );
}

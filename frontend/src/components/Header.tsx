import { useDispatch } from 'react-redux';
import { NavLink, useNavigate } from 'react-router-dom';
import { apiSlice } from '../api/apiSlice';
import { logout } from '../features/auth/authSlice';

export function Header() {
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const handleLogout = () => {
    dispatch(logout());
    // Clears RTK Query's cache too — otherwise the next login in this tab could briefly show
    // this account's cached feed/explore data before its own requests resolve.
    dispatch(apiSlice.util.resetApiState());
    navigate('/login');
  };

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
        <button type="button" className="ghost-button" onClick={handleLogout}>
          Log out
        </button>
      </nav>
    </header>
  );
}

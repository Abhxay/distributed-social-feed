import { useDispatch } from 'react-redux';
import { NavLink, useNavigate } from 'react-router-dom';
import { logout } from '../features/auth/authSlice';

export function Header() {
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const handleLogout = () => {
    dispatch(logout());
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

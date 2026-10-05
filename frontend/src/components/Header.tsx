import { useDispatch } from 'react-redux';
import { useNavigate } from 'react-router-dom';
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
      <button type="button" className="ghost-button" onClick={handleLogout}>
        Log out
      </button>
    </header>
  );
}

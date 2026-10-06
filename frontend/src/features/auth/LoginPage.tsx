import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { apiSlice, useLoginMutation } from '../../api/apiSlice';
import { setTokens } from './authSlice';

export function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [login, { isLoading, error }] = useLoginMutation();
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const tokens = await login({ username, password }).unwrap();
    dispatch(setTokens(tokens));
    // Without this, RTK Query's cache (keyed by endpoint, not by user — getFeed takes no args)
    // would keep showing whichever user's data was cached from a previous login in this tab.
    dispatch(apiSlice.util.resetApiState());
    navigate('/feed');
  };

  return (
    <div className="auth-screen">
      <form onSubmit={handleSubmit} className="auth-form">
        <h1>Log in</h1>

        <label htmlFor="login-username">Username</label>
        <input
          id="login-username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          autoComplete="username"
        />

        <label htmlFor="login-password">Password</label>
        <input
          id="login-password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
        />

        <button type="submit" disabled={isLoading}>
          Log in
        </button>

        {error && <p role="alert">Invalid username or password</p>}

        <p className="auth-switch">
          No account? <Link to="/signup">Sign up</Link>
        </p>
      </form>
    </div>
  );
}

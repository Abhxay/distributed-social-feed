import { useState } from 'react';
import type { FormEvent } from 'react';
import { useCheckUsernameQuery, useSignupMutation } from '../../api/apiSlice';
import { useDebouncedValue } from '../../hooks/useDebouncedValue';

export function SignupPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const debouncedUsername = useDebouncedValue(username, 400);

  const { data: availability, isFetching: checkingAvailability } = useCheckUsernameQuery(
    debouncedUsername,
    { skip: debouncedUsername.length < 3 },
  );
  const [signup, { isLoading, isSuccess, error }] = useSignupMutation();

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    signup({ username, password });
  };

  return (
    <form onSubmit={handleSubmit} className="auth-form">
      <h1>Sign up</h1>

      <label htmlFor="signup-username">Username</label>
      <input
        id="signup-username"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        autoComplete="username"
      />
      {debouncedUsername.length >= 3 && !checkingAvailability && availability && (
        <p role="status">
          {availability.available ? 'Username is available' : 'Username is taken'}
        </p>
      )}

      <label htmlFor="signup-password">Password</label>
      <input
        id="signup-password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        autoComplete="new-password"
      />

      <button type="submit" disabled={isLoading}>
        Sign up
      </button>

      {isSuccess && <p role="status">Account created</p>}
      {error && <p role="alert">Signup failed</p>}
    </form>
  );
}

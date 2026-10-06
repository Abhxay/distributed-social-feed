import { useState } from 'react';
import type { FormEvent } from 'react';
import { useChangePasswordMutation } from '../../api/apiSlice';
import { Header } from '../../components/Header';

export function AccountSettingsPage() {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [mismatch, setMismatch] = useState(false);
  const [changePassword, { isLoading, isSuccess, error }] = useChangePasswordMutation();

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (newPassword !== confirmPassword) {
      setMismatch(true);
      return;
    }
    setMismatch(false);
    await changePassword({ currentPassword, newPassword });
    setCurrentPassword('');
    setNewPassword('');
    setConfirmPassword('');
  };

  return (
    <div className="feed-page">
      <Header />
      <main className="feed-main">
        <form onSubmit={handleSubmit} className="auth-form settings-form">
          <h1>Account settings</h1>

          <label htmlFor="current-password">Current password</label>
          <input
            id="current-password"
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
          />

          <label htmlFor="new-password">New password</label>
          <input
            id="new-password"
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
          />

          <label htmlFor="confirm-password">Confirm new password</label>
          <input
            id="confirm-password"
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
          />

          <button type="submit" disabled={isLoading}>
            Change password
          </button>

          {mismatch && <p role="alert">New passwords don't match</p>}
          {error && <p role="alert">Current password is incorrect</p>}
          {isSuccess && <p role="status">Password updated</p>}

          <p className="auth-switch">
            Forgot your password? <button type="button" className="link-button" disabled>Coming soon</button>
          </p>
        </form>
      </main>
    </div>
  );
}

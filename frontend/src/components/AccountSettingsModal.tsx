import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { useChangePasswordMutation, useGetProfileQuery } from '../api/apiSlice';
import { CloseIcon, LockIcon } from './Icons';

export function AccountSettingsModal({ open, onClose }: { open: boolean; onClose: () => void }) {
  const { data: profile } = useGetProfileQuery();
  const [changePassword, { isLoading }] = useChangePasswordMutation();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [status, setStatus] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  useEffect(() => {
    if (!open) {
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setStatus(null);
    }
  }, [open]);

  useEffect(() => {
    if (!open) return;
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', handleKeyDown);
    return () => document.removeEventListener('keydown', handleKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setStatus(null);

    if (newPassword !== confirmPassword) {
      setStatus({ type: 'error', text: 'New passwords do not match.' });
      return;
    }
    if (newPassword.length < 6) {
      setStatus({ type: 'error', text: 'New password must be at least 6 characters.' });
      return;
    }

    try {
      await changePassword({ currentPassword, newPassword }).unwrap();
      setStatus({ type: 'success', text: 'Password successfully updated.' });
      setTimeout(onClose, 1500);
    } catch {
      setStatus({ type: 'error', text: 'Current password is incorrect.' });
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-panel" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <div className="modal-title">
            <LockIcon size={18} />
            <h2>Account settings</h2>
          </div>
          <button type="button" className="modal-close" onClick={onClose} aria-label="Close settings">
            <CloseIcon size={18} />
          </button>
        </div>

        {profile && (
          <div className="info-chip">
            <div className="muted">Authenticated user:</div>
            <div className="info-chip-value">@{profile.username}</div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="settings-form">
          <label htmlFor="current-password">Current password (required)</label>
          <input
            id="current-password"
            type="password"
            required
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
          />

          <label htmlFor="new-password">New password</label>
          <input
            id="new-password"
            type="password"
            placeholder="Minimum 6 characters"
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

          {status && <p className={status.type === 'success' ? 'status-ok' : 'status-bad'}>{status.text}</p>}

          <div className="modal-actions">
            <button type="button" className="ghost-button" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" disabled={isLoading || !currentPassword || !newPassword}>
              {isLoading ? 'Updating…' : 'Update password'}
            </button>
          </div>
        </form>

        <div className="forgot-password-row">
          <div>
            <div className="forgot-password-title">Email recovery</div>
            <div className="muted forgot-password-note">
              Pending OTP / transactional email verification infrastructure.
            </div>
          </div>
          <button type="button" className="link-button" disabled title="Coming soon">
            Forgot password (coming soon)
          </button>
        </div>
      </div>
    </div>
  );
}

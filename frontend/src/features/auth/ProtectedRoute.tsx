import type { ReactElement } from 'react';
import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { selectAccessToken } from './authSlice';

export function ProtectedRoute({ children }: { children: ReactElement }) {
  const accessToken = useSelector(selectAccessToken);
  if (!accessToken) {
    return <Navigate to="/login" replace />;
  }
  return children;
}

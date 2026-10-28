import React, { createContext, useContext, useMemo, useState } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { homeFor, type Role } from './api';

type Auth = { token: string | null; username: string; role: Role; login: (token: string, username: string, role?: string) => void; logout: () => void };
const AuthContext = createContext<Auth>(null!);
export const useAuth = () => useContext(AuthContext);

function decode(token: string) { try { return JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))); } catch { return {}; } }
const clean = (r: unknown) => String(r || 'USER').replace('ROLE_', '') as Role;

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [token, setToken] = useState(sessionStorage.getItem('waslah_token'));
  const [username, setUsername] = useState(sessionStorage.getItem('waslah_user') || '');
  const role = useMemo(() => {
    const c = token ? decode(token) : {};
    return clean(c.role || c.roles?.[0] || sessionStorage.getItem('waslah_role'));
  }, [token]);
  const login = (t: string, u: string, r?: string) => {
    sessionStorage.setItem('waslah_token', t); sessionStorage.setItem('waslah_user', u);
    if (r) sessionStorage.setItem('waslah_role', clean(r));
    setToken(t); setUsername(u);
  };
  const logout = () => {
    ['waslah_token', 'waslah_user', 'waslah_role'].forEach(k => sessionStorage.removeItem(k));
    setToken(null); setUsername('');
  };
  return <AuthContext.Provider value={{ token, username, role, login, logout }}>{children}</AuthContext.Provider>;
}

/** Signed-in users with one of the allowed roles; anyone else goes to their own home. */
export function RequireRole({ allow }: { allow: Role[] }) {
  const { token, role } = useAuth(); const loc = useLocation();
  if (!token) return <Navigate to="/login" replace state={{ from: loc.pathname }} />;
  return allow.includes(role) ? <Outlet /> : <Navigate to={homeFor(role)} replace />;
}

/** Any signed-in user (used for pages shared by all roles, e.g. the profile). */
export function RequireAuth() {
  const { token } = useAuth(); const loc = useLocation();
  return token ? <Outlet /> : <Navigate to="/login" replace state={{ from: loc.pathname }} />;
}

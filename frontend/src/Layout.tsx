import React, { useState } from 'react';
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BriefcaseBusiness, CalendarDays, ClipboardList, Coffee, LayoutDashboard, LogOut, Menu, UserRound, UsersRound, X, Bell, ShieldCheck, Handshake, ScrollText, ListChecks, Store, type LucideIcon } from 'lucide-react';
import { api, fmt, homeFor, list, type AppNotification, type Page, type Role } from './api';
import { useAuth } from './auth';
import { Avatar, useUi } from './ui';
import { useNotificationStream, useProfileImage } from './hooks';

type Item = [string, string, LucideIcon, boolean?];
const MENUS: Record<Role, { label: string; area: string; items: Item[] }> = {
  USER: { label: 'YOUR SPACE', area: 'Member', items: [
    ['/dashboard', 'Dashboard', LayoutDashboard, true], ['/jobs', 'Job offers', BriefcaseBusiness], ['/applications', 'My applications', ClipboardList],
    ['/cafes', 'Cafés', Coffee], ['/bookings', 'My bookings', CalendarDays], ['/sessions', 'Study sessions', UsersRound],
    ['/partner', 'Partner with us', Handshake], ['/profile', 'My profile', UserRound]] },
  EMPLOYER: { label: 'EMPLOYER AREA', area: 'Employer', items: [
    ['/employer', 'Overview', LayoutDashboard, true], ['/employer/jobs', 'My jobs & applicants', BriefcaseBusiness], ['/profile', 'My profile', UserRound]] },
  CAFE_MANAGER: { label: 'CAFÉ MANAGER AREA', area: 'Café manager', items: [
    ['/manager', 'Overview', LayoutDashboard, true], ['/manager/spaces', 'Cafés & workspaces', Store], ['/manager/bookings', 'Booking requests', CalendarDays], ['/profile', 'My profile', UserRound]] },
  ADMIN: { label: 'ADMINISTRATION', area: 'Admin', items: [
    ['/admin', 'Dashboard', LayoutDashboard, true], ['/admin/users', 'Users', UsersRound], ['/admin/requests', 'Requests', ListChecks], ['/admin/audit', 'Audit log', ScrollText], ['/profile', 'My profile', UserRound]] },
};

function Bell_() {
  const { toast } = useUi(); const qc = useQueryClient(); const [open, setOpen] = useState(false);
  const count = useQuery({ queryKey: ['notifications', 'count'], refetchInterval: 60000, queryFn: async () => (await api.get<{ unread: number }>('/api/notifications/unread-count')).data.unread });
  const items = useQuery({ queryKey: ['notifications', 'list'], enabled: open, queryFn: async () => list<AppNotification>((await api.get<Page<AppNotification>>('/api/notifications', { params: { size: 8 } })).data) });
  const read = useMutation({ mutationFn: (id: number) => api.put(`/api/notifications/${id}/read`), onSuccess: () => qc.invalidateQueries({ queryKey: ['notifications'] }) });
  useNotificationStream((_name, data) => {
    if (!data) return;
    let text = data;
    try { const j = JSON.parse(data); text = typeof j === 'string' ? j : (j.message || 'You have a new notification'); } catch { /* plain text event */ }
    toast(text);
  });
  return <div className="bell-wrap">
    <button className="icon-btn" aria-label="Notifications" onClick={() => setOpen(!open)}><Bell size={20} />{!!count.data && <span className="bell-dot">{count.data > 9 ? '9+' : count.data}</span>}</button>
    {open && <div className="bell-menu"><strong>Notifications</strong>
      {items.data?.length === 0 && <p className="muted">You're all caught up.</p>}
      {items.data?.map(n => <button key={n.id} className={'bell-item ' + (n.read ? '' : 'unread')} onClick={() => !n.read && read.mutate(n.id)}><span>{n.message}</span><small>{fmt(n.createdAt)}</small></button>)}
    </div>}
  </div>;
}

export default function Layout() {
  const a = useAuth(); const nav = useNavigate(); const [open, setOpen] = useState(false);
  const img = useProfileImage(); const menu = MENUS[a.role] || MENUS.USER;
  return <div className={'shell area-' + a.role.toLowerCase()}>
    <aside className={'sidebar ' + (open ? 'sidebar-open' : '')}>
      <Link to={homeFor(a.role)} className="brand" onClick={() => setOpen(false)}><span className="brand-mark">w.</span><span>waslah<span className="brand-dot">.</span><small>Connect · Learn · Grow</small></span></Link>
      <div className="sidebar-label">{menu.label}</div>
      <nav>{menu.items.map(([to, label, Icon, end]) => <NavLink key={to + label} to={to} end={end} onClick={() => setOpen(false)} className={({ isActive }) => 'nav-link ' + (isActive ? 'active' : '')}><Icon size={19} />{label}</NavLink>)}</nav>
      <div className="sidebar-bottom"><Avatar name={a.username || '?'} src={img.data} /><div><strong>{a.username}</strong><small>{menu.area}</small></div>
        <button title="Sign out" className="icon-btn" onClick={() => { a.logout(); nav('/'); }}><LogOut size={19} /></button></div>
    </aside>
    <div className="main-area">
      <header className="topbar"><button className="icon-btn mobile-menu" onClick={() => setOpen(!open)}>{open ? <X /> : <Menu />}</button>
        <div className="topbar-title">{a.role === 'ADMIN' ? <><ShieldCheck size={16} /> Administration console</> : <>Your space to connect and grow <span>✦</span></>}</div>
        <div className="topbar-right"><span className="pill">{menu.area}</span><Bell_ /><Link to="/profile"><Avatar name={a.username || '?'} src={img.data} /></Link></div></header>
      <main className="content"><Outlet /></main>
    </div>
    {open && <div className="overlay" onClick={() => setOpen(false)} />}
  </div>;
}

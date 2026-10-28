import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api, type AdminStats } from '../../api';
import { PageHead, Panel, QueryState, StatCard } from '../../ui';

export default function AdminDashboard() {
  const q = useQuery({ queryKey: ['admin-stats'], refetchInterval: 30000, queryFn: async () => (await api.get<AdminStats>('/api/admin/stats')).data });
  const s = q.data;
  return <>
    <PageHead eyebrow="ADMINISTRATION" title="Platform overview" sub="Everything that needs your attention, at a glance." />
    <QueryState loading={q.isLoading} error={q.error} />
    {s && <>
      <div className="stat-grid">
        <StatCard label="Employer requests waiting" value={s.pendingEmployerRequests} tone={s.pendingEmployerRequests ? 'alert' : undefined} hint="Review in Requests" />
        <StatCard label="Café requests waiting" value={s.pendingCafeRequests} tone={s.pendingCafeRequests ? 'alert' : undefined} hint="Review in Requests" />
        <StatCard label="Pending bookings" value={s.pendingBookings} tone="warm" />
        <StatCard label="Confirmed bookings" value={s.confirmedBookings} />
        <StatCard label="Open jobs" value={s.openJobs} />
        <StatCard label="Active study sessions" value={s.activeStudySessions} />
      </div>
      <div className="detail-layout">
        <Panel title="Users" action={<Link className="text-btn" to="/admin/users">Manage users →</Link>}>
          <div className="facts"><div><span>Total</span><strong>{s.totalUsers}</strong></div><div><span>Active</span><strong>{s.activeUsers}</strong></div><div><span>Deactivated</span><strong>{s.inactiveUsers}</strong></div></div>
          <div className="bars">{Object.entries(s.usersByRole || {}).map(([role, n]) => <div key={role} className="bar-row"><span>{role.replace('_', ' ')}</span>
            <div className="bar"><i style={{ width: `${s.totalUsers ? (n / s.totalUsers) * 100 : 0}%` }} /></div><b>{n}</b></div>)}</div>
        </Panel>
        <Panel title="Shortcuts">
          <div className="list-stack"><Link className="list-row" to="/admin/requests"><div className="list-main"><strong>Approve requests</strong><span>Employers and café owners</span></div>→</Link>
            <Link className="list-row" to="/admin/audit"><div className="list-main"><strong>Audit log</strong><span>Who did what, and when</span></div>→</Link></div>
        </Panel>
      </div>
    </>}
  </>;
}

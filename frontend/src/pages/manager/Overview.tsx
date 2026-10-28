import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { api, fmt, type Booking, type Cafe } from '../../api';
import { useAuth } from '../../auth';
import { PageHead, Panel, QueryState, StatCard, StatusBadge } from '../../ui';

export default function ManagerOverview() {
  const { username } = useAuth();
  const cafes = useQuery({ queryKey: ['my-cafes'], queryFn: async () => (await api.get<Cafe[]>('/api/cafes/my')).data });
  const bookings = useQuery({ queryKey: ['manager-bookings'], queryFn: async () => (await api.get<Booking[]>('/api/manager/bookings')).data });
  const pending = bookings.data?.filter(b => b.status === 'PENDING') ?? [];
  return <>
    <PageHead eyebrow="CAFÉ MANAGER AREA" title={`Welcome, ${username}.`} sub="Your cafés, workspaces and booking requests." />
    <QueryState loading={cafes.isLoading} error={cafes.error} />
    <div className="stat-grid">
      <StatCard label="My cafés" value={cafes.data?.length ?? '–'} />
      <StatCard label="Requests to review" value={pending.length} tone={pending.length ? 'alert' : undefined} />
      <StatCard label="Confirmed bookings" value={bookings.data?.filter(b => b.status === 'CONFIRMED').length ?? '–'} />
    </div>
    <Panel title="Waiting for your decision" action={<Link className="text-btn" to="/manager/bookings">All requests →</Link>}>
      <div className="list-stack">{pending.slice(0, 5).map(b => <Link to="/manager/bookings" className="list-row" key={b.id}><div className="list-main"><strong>Booking #{b.id} · {b.username}</strong><span>{fmt(b.startTime)} — {fmt(b.endTime)}</span></div><StatusBadge value={b.status} /></Link>)}</div>
      {pending.length === 0 && <p className="muted">No booking requests are waiting.</p>}
    </Panel>
  </>;
}

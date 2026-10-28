import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowRight, BriefcaseBusiness, CalendarDays, ClipboardList, Coffee, UsersRound } from 'lucide-react';
import { api, list, type Application, type Booking, type Page } from '../../api';
import { useAuth } from '../../auth';
import { PageHead, StatCard } from '../../ui';

function Feature({ icon, title, desc, to }: { icon: React.ReactNode; title: string; desc: string; to: string }) {
  return <Link to={to} className="feature"><div className="feature-icon">{icon}</div><h3>{title}</h3><p>{desc}</p><span>Open <ArrowRight size={16} /></span></Link>;
}

export default function Dashboard() {
  const { username } = useAuth();
  const bookings = useQuery({ queryKey: ['bookings'], queryFn: async () => list<Booking>((await api.get<Page<Booking>>('/api/bookings', { params: { size: 100 } })).data) });
  const apps = useQuery({ queryKey: ['applications'], queryFn: async () => (await api.get<Application[]>('/api/applications/my')).data });
  const confirmed = bookings.data?.filter(b => b.status === 'CONFIRMED').length ?? '–';
  const pending = bookings.data?.filter(b => b.status === 'PENDING').length ?? '–';
  const waiting = apps.data?.filter(a => a.status === 'PENDING').length ?? '–';
  return <>
    <PageHead eyebrow="YOUR DASHBOARD" title={`Good to see you, ${username}.`} sub="Where would you like to go today?" />
    <div className="stat-grid">
      <StatCard label="Confirmed bookings" value={confirmed} hint="Ready to host a session" />
      <StatCard label="Waiting for the café" value={pending} hint="Pending bookings" tone="warm" />
      <StatCard label="Applications in review" value={waiting} hint={`${apps.data?.length ?? 0} sent in total`} />
    </div>
    <div className="dashboard-hero"><div><span className="eyebrow">A LITTLE INSPIRATION</span><h2>Big things start with<br />small connections.</h2>
      <p>Discover a new opportunity or make space for your next great idea.</p><Link className="button light" to="/sessions">Explore study sessions <ArrowRight size={16} /></Link></div><div className="dashboard-decoration">✳</div></div>
    <div className="section-head"><h2>Quick access</h2></div>
    <div className="feature-grid">
      <Feature icon={<BriefcaseBusiness />} title="Job offers" desc="Browse open roles with salary and deadline." to="/jobs" />
      <Feature icon={<Coffee />} title="Book a study space" desc="Find a place to focus and create." to="/cafes" />
      <Feature icon={<UsersRound />} title="Join a session" desc="Learn and grow with others." to="/sessions" />
      <Feature icon={<CalendarDays />} title="My bookings" desc="Confirm status and start a session." to="/bookings" />
      <Feature icon={<ClipboardList />} title="My applications" desc="Follow every application." to="/applications" />
    </div>
  </>;
}

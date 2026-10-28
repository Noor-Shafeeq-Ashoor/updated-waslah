import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CalendarDays, Plus } from 'lucide-react';
import { api, fmt, list, type Booking, type Page } from '../../api';
import { Empty, Notice, PageHead, QueryState, StatusBadge, useUi } from '../../ui';
import { errorText } from '../../api';

export default function Bookings() {
  const { toast, confirm } = useUi(); const qc = useQueryClient();
  const q = useQuery({ queryKey: ['bookings'], queryFn: async () => list<Booking>((await api.get<Page<Booking>>('/api/bookings', { params: { size: 100 } })).data) });
  const m = useMutation({ mutationFn: (id: number) => api.put(`/api/bookings/${id}/cancel`), onSuccess: () => { toast('Booking cancelled.'); qc.invalidateQueries({ queryKey: ['bookings'] }); qc.invalidateQueries({ queryKey: ['sessions'] }); } });
  return <>
    <PageHead title="My bookings" sub="Your study spaces and reservations, all in one place." />
    <Notice text={m.error ? errorText(m.error) : ''} />
    <QueryState loading={q.isLoading} error={q.error} />
    <div className="list-stack">{q.data?.map(b => {
      const canHost = b.status === 'CONFIRMED' && b.studySpaceCapacity >= 2 && new Date(b.endTime) > new Date();
      return <div className="list-row" key={b.id}><div className="card-icon"><CalendarDays /></div>
        <div className="list-main"><strong>Booking #{b.id} · Space #{b.studySpaceId}</strong><span>{fmt(b.startTime)} — {fmt(b.endTime)} · {b.studySpaceCapacity} seat{b.studySpaceCapacity === 1 ? '' : 's'}</span></div>
        <StatusBadge value={b.status} />
        {canHost && <Link className="button small" to={'/sessions/create?booking=' + b.id}><Plus size={15} /> Create session</Link>}
        {b.status === 'CONFIRMED' && b.studySpaceCapacity < 2 && <span className="muted tiny">Single seat: no sessions</span>}
        {['PENDING', 'CONFIRMED'].includes(b.status) && <button className="text-btn" onClick={async () => { if (await confirm('Cancel this booking? A linked study session will be cancelled too.')) m.mutate(b.id); }}>Cancel</button>}
      </div>;
    })}</div>
    {q.data?.length === 0 && <Empty message="No bookings yet. Explore cafés to get started." />}
  </>;
}

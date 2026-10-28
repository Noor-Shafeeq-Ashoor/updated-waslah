import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { CalendarDays } from 'lucide-react';
import { api, errorText, fmt, type Booking } from '../../api';
import { Button, Empty, Notice, PageHead, QueryState, StatusBadge, useUi } from '../../ui';

export default function ManagerBookings() {
  const { toast, confirm } = useUi(); const qc = useQueryClient(); const [filter, setFilter] = useState('PENDING');
  const q = useQuery({ queryKey: ['manager-bookings'], queryFn: async () => (await api.get<Booking[]>('/api/manager/bookings')).data });
  const m = useMutation({ mutationFn: ({ id, action }: { id: number; action: 'confirm' | 'cancel' }) => api.put(`/api/manager/bookings/${id}/${action}`),
    onSuccess: (_r, v) => { toast(v.action === 'confirm' ? 'Booking confirmed.' : 'Booking declined.'); qc.invalidateQueries({ queryKey: ['manager-bookings'] }); } });
  const rows = q.data?.filter(b => filter === 'ALL' || b.status === filter);
  return <>
    <PageHead eyebrow="CAFÉ MANAGER AREA" title="Booking requests" sub="Confirm or decline bookings for your workspaces." />
    <div className="chips">{['PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'ALL'].map(s => <button key={s} className={'chip ' + (filter === s ? 'on' : '')} onClick={() => setFilter(s)}>{s}</button>)}</div>
    <Notice text={m.error ? errorText(m.error) : ''} />
    <QueryState loading={q.isLoading} error={q.error} />
    <div className="list-stack">{rows?.map(b => <div className="list-row" key={b.id}><div className="card-icon"><CalendarDays /></div>
      <div className="list-main"><strong>Booking #{b.id} · {b.username}</strong><span>Space #{b.studySpaceId} ({b.studySpaceCapacity} seats) · {fmt(b.startTime)} — {fmt(b.endTime)}</span></div><StatusBadge value={b.status} />
      {b.status === 'PENDING' && <Button busy={m.isPending} onClick={() => m.mutate({ id: b.id, action: 'confirm' })}>Confirm</Button>}
      {['PENDING', 'CONFIRMED'].includes(b.status) && <button className="text-btn danger" onClick={async () => { if (await confirm(b.status === 'PENDING' ? 'Decline this booking?' : 'Cancel this confirmed booking? The linked study session will be cancelled and its members notified.')) m.mutate({ id: b.id, action: 'cancel' }); }}>{b.status === 'PENDING' ? 'Decline' : 'Cancel'}</button>}</div>)}</div>
    {rows?.length === 0 && <Empty message="Nothing in this list." />}
  </>;
}

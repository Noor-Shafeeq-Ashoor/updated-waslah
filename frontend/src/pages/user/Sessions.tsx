import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Clock, History, Plus, RefreshCw, UserRound, UsersRound } from 'lucide-react';
import { api, errorText, fmt, list, type Booking, type Page, type Session, type SessionEvent } from '../../api';
import { Button, Empty, Notice, PageHead, Panel, QueryState, StatusBadge, useUi } from '../../ui';

function History_({ id }: { id: number }) {
  const q = useQuery({ queryKey: ['session-events', id], retry: false, queryFn: async () => (await api.get<SessionEvent[]>(`/api/study-sessions/${id}/events`)).data });
  if (q.isLoading) return <p className="muted">Loading history…</p>;
  if (q.error) return <p className="muted">History is visible to participants only.</p>;
  return <ul className="timeline">{q.data?.map(e => <li key={e.id}><b>{e.eventType.replaceAll('_', ' ')}</b>{e.username && ` · ${e.username}`}<small>{fmt(e.occurredAt)}</small></li>)}</ul>;
}

export function Sessions() {
  const { toast } = useUi(); const qc = useQueryClient();
  const [filter, setFilter] = useState('ALL'); const [open, setOpen] = useState<number | null>(null); const [busy, setBusy] = useState<number | null>(null);
  const q = useQuery({ queryKey: ['sessions'], refetchInterval: 15000, queryFn: async () => (await api.get<Session[]>('/api/study-sessions')).data });
  async function act(id: number, kind: 'join' | 'leave') {
    setBusy(id);
    try {
      const r = await api.post(`/api/study-sessions/${id}/${kind}`);
      toast(kind === 'join' ? (r.data?.message || `Status: ${r.data?.status}`) : 'You left the session.');
      qc.invalidateQueries({ queryKey: ['sessions'] }); qc.invalidateQueries({ queryKey: ['session-events', id] });
    } catch (e) { toast(errorText(e), 'error'); } finally { setBusy(null); }
  }
  const rows = q.data?.filter(s => filter === 'ALL' || s.status === filter);
  return <>
    <PageHead title="Better together" sub="Find a study session and learn with your community." />
    <div className="page-actions"><Link className="button" to="/sessions/create"><Plus size={17} /> Create session</Link>
      <button className="button outline" onClick={() => q.refetch()}><RefreshCw size={16} /> Refresh</button></div>
    <div className="chips">{['ALL', 'OPEN', 'FULL', 'COMPLETED', 'CANCELLED'].map(t => <button key={t} className={'chip ' + (filter === t ? 'on' : '')} onClick={() => setFilter(t)}>{t}</button>)}</div>
    <QueryState loading={q.isLoading} error={q.error} />
    <div className="card-grid">{rows?.map(s => <article className="item-card" key={s.id}>
      <StatusBadge value={s.status} /><h3>{s.title}</h3><p>{s.description || 'A place to connect and learn.'}</p>
      <div className="meta"><UserRound size={15} />Hosted by {s.organizerUsername}</div>
      <div className="meta"><Clock size={15} />{fmt(s.startTime)}</div>
      <div className="meta"><UsersRound size={15} /><b>{s.participantCount}/{s.capacity}</b>&nbsp;seats taken{s.waitlistCount > 0 && <> · {s.waitlistCount} waiting</>}</div>
      <div className="seatbar" aria-label={`${s.participantCount} of ${s.capacity} seats taken`}><i style={{ width: `${Math.min(100, (s.participantCount / s.capacity) * 100)}%` }} /></div>
      <div className="card-footer"><div className="inline-actions">
        <Button busy={busy === s.id} disabled={!['OPEN', 'FULL'].includes(s.status)} onClick={() => act(s.id, 'join')}>{s.status === 'FULL' ? 'Join waitlist' : 'Join'}</Button>
        <button className="text-btn" disabled={busy === s.id} onClick={() => act(s.id, 'leave')}>Leave</button>
        <button className="text-btn" onClick={() => setOpen(open === s.id ? null : s.id)}><History size={14} /> History</button></div></div>
      {open === s.id && <History_ id={s.id} />}
    </article>)}</div>
    {rows?.length === 0 && <Empty message="No study sessions here yet. Be the first to create one!" />}
    <p className="muted">The list refreshes every 15 seconds. Seats and the waitlist are decided by the server, so a full session puts you in the queue automatically.</p>
  </>;
}

export function CreateSession() {
  const nav = useNavigate(); const { toast } = useUi(); const qc = useQueryClient(); const [params] = useSearchParams();
  const b = useQuery({ queryKey: ['bookings'], queryFn: async () => list<Booking>((await api.get<Page<Booking>>('/api/bookings', { params: { size: 100 } })).data) });
  const eligible = b.data?.filter(x => x.status === 'CONFIRMED' && x.studySpaceCapacity >= 2 && new Date(x.endTime) > new Date()) ?? [];
  const [bookingId, setBookingId] = useState(params.get('booking') || ''); const [title, setTitle] = useState(''); const [description, setDescription] = useState(''); const [capacity, setCapacity] = useState(2);
  const chosen = eligible.find(x => String(x.id) === bookingId); const max = chosen?.studySpaceCapacity ?? 100;
  const m = useMutation({
    mutationFn: () => api.post('/api/study-sessions', { bookingId: Number(bookingId), title, description, capacity }),
    onSuccess: () => { toast('Study session created.'); qc.invalidateQueries({ queryKey: ['sessions'] }); nav('/sessions'); },
  });
  return <>
    <PageHead title="Create a study session" sub="Bring people together around a confirmed booking." />
    {!b.isLoading && eligible.length === 0 ? <Panel title="No eligible booking yet">
      <p>You can host a session only when you have a <b>confirmed</b> booking, still in the future, in a space with <b>2 or more seats</b>.</p>
      <Link className="button" to="/cafes">Book a group space</Link></Panel> :
    <Panel title="Session details"><QueryState loading={b.isLoading} error={b.error} />
      <form className="form-stack" onSubmit={e => { e.preventDefault(); m.mutate(); }}>
        <label className="field">Confirmed booking<select required value={bookingId} onChange={e => { setBookingId(e.target.value); setCapacity(2); }}>
          <option value="">Choose booking</option>{eligible.map(x => <option value={x.id} key={x.id}>Booking #{x.id} · {fmt(x.startTime)} · {x.studySpaceCapacity} seats</option>)}</select></label>
        <label className="field">Session title<input required maxLength={150} value={title} onChange={e => setTitle(e.target.value)} /></label>
        <label className="field">Description<textarea maxLength={1000} rows={4} value={description} onChange={e => setDescription(e.target.value)} /></label>
        <label className="field">Seats (2 to {max})<input type="number" min={2} max={max} value={capacity} onChange={e => setCapacity(Number(e.target.value))} /></label>
        <Notice text={m.error ? errorText(m.error) : ''} />
        <Button busy={m.isPending} type="submit" disabled={capacity < 2 || capacity > max}>Create session</Button>
      </form></Panel>}
  </>;
}

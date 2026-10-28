import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Pencil, Plus, UsersRound } from 'lucide-react';
import { api, errorText, type Cafe, type Space } from '../../api';
import { Button, Empty, Notice, PageHead, Panel, QueryState, StatusBadge, useUi } from '../../ui';

function SpaceRow({ cafeId, s }: { cafeId: number; s: Space }) {
  const { toast } = useUi(); const qc = useQueryClient(); const [edit, setEdit] = useState(false);
  const [f, setF] = useState({ name: s.name, capacity: String(s.capacity), pricePerHour: String(s.pricePerHour ?? 0) });
  const refresh = () => qc.invalidateQueries({ queryKey: ['cafe-spaces', cafeId] });
  const save = useMutation({ mutationFn: () => api.put(`/api/cafes/${cafeId}/study-spaces/${s.id}`, { name: f.name, capacity: Number(f.capacity), pricePerHour: Number(f.pricePerHour) }), onSuccess: () => { toast('Workspace updated.'); setEdit(false); refresh(); } });
  const avail = useMutation({ mutationFn: () => api.patch(`/api/cafes/${cafeId}/study-spaces/${s.id}/availability`, { available: !s.available }), onSuccess: () => { toast(s.available ? 'Workspace hidden from bookings.' : 'Workspace is bookable again.'); refresh(); } });
  return <div className="space-row">
    {edit ? <form className="form-grid" onSubmit={e => { e.preventDefault(); save.mutate(); }}>
      <label className="field">Name<input required value={f.name} onChange={e => setF({ ...f, name: e.target.value })} /></label>
      <label className="field">Seats (1–100)<input required type="number" min={1} max={100} value={f.capacity} onChange={e => setF({ ...f, capacity: e.target.value })} /></label>
      <label className="field">Price per hour (BHD)<input required type="number" min={0} step="0.5" value={f.pricePerHour} onChange={e => setF({ ...f, pricePerHour: e.target.value })} /></label>
      <div className="full"><Notice text={save.error ? errorText(save.error) : ''} /><div className="inline-actions"><Button busy={save.isPending} type="submit">Save</Button><button type="button" className="text-btn" onClick={() => setEdit(false)}>Cancel</button></div></div></form>
    : <div className="list-row flat"><div className="list-main"><strong>{s.name}</strong><span><UsersRound size={14} /> {s.capacity} seat{s.capacity === 1 ? "" : "s"} · {s.pricePerHour} BHD/hour</span></div>
      <StatusBadge value={s.available ? 'AVAILABLE' : 'HIDDEN'} />
      <button className="text-btn" disabled={avail.isPending} onClick={() => avail.mutate()}>{s.available ? 'Hide' : 'Show'}</button>
      <button className="icon-btn" title="Edit" onClick={() => setEdit(true)}><Pencil size={17} /></button></div>}
    {avail.error && <Notice text={errorText(avail.error)} />}
  </div>;
}

function CafeSpaces({ cafe }: { cafe: Cafe }) {
  const { toast } = useUi(); const qc = useQueryClient(); const [adding, setAdding] = useState(false);
  const [f, setF] = useState({ name: '', capacity: '4', pricePerHour: '3' });
  const q = useQuery({ queryKey: ['cafe-spaces', cafe.id], queryFn: async () => (await api.get<Space[]>(`/api/cafes/${cafe.id}/study-spaces`)).data });
  const add = useMutation({ mutationFn: () => api.post(`/api/cafes/${cafe.id}/study-spaces`, { name: f.name, capacity: Number(f.capacity), pricePerHour: Number(f.pricePerHour) }),
    onSuccess: () => { toast('Workspace added.'); setAdding(false); setF({ name: '', capacity: '4', pricePerHour: '3' }); qc.invalidateQueries({ queryKey: ['cafe-spaces', cafe.id] }); } });
  return <Panel title={cafe.name} action={<Button onClick={() => setAdding(!adding)}><Plus size={16} /> Add workspace</Button>}>
    <p className="muted">{cafe.location}</p>
    {adding && <form className="form-grid add-box" onSubmit={e => { e.preventDefault(); add.mutate(); }}>
      <label className="field">Name<input required value={f.name} onChange={e => setF({ ...f, name: e.target.value })} /></label>
      <label className="field">Seats<input required type="number" min={1} max={100} value={f.capacity} onChange={e => setF({ ...f, capacity: e.target.value })} /></label>
      <label className="field">Price per hour (BHD)<input required type="number" min={0} step="0.5" value={f.pricePerHour} onChange={e => setF({ ...f, pricePerHour: e.target.value })} /></label>
      <div className="full"><Notice text={add.error ? errorText(add.error) : ''} /><Button busy={add.isPending} type="submit">Add workspace</Button></div></form>}
    <QueryState loading={q.isLoading} error={q.error} />
    {q.data?.map(s => <SpaceRow key={s.id} cafeId={cafe.id} s={s} />)}
    {q.data?.length === 0 && <Empty message="No workspaces yet. Add the first one." />}
    <p className="muted tiny">Seats can't be set lower than a running study session needs. Spaces with 2 or more seats can host study sessions.</p>
  </Panel>;
}

export default function ManagerSpaces() {
  const q = useQuery({ queryKey: ['my-cafes'], queryFn: async () => (await api.get<Cafe[]>('/api/cafes/my')).data });
  return <>
    <PageHead eyebrow="CAFÉ MANAGER AREA" title="Cafés & workspaces" sub="Add workspaces, change their seats and price, and hide them when needed." />
    <QueryState loading={q.isLoading} error={q.error} />
    {q.data?.map(c => <CafeSpaces key={c.id} cafe={c} />)}
    {q.data?.length === 0 && <Empty message="You don't manage a café yet." />}
  </>;
}

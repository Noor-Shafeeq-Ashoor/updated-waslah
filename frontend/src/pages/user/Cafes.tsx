import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowRight, Coffee, MapPin, UsersRound } from 'lucide-react';
import { api, errorText, list, type Cafe, type Page, type Space } from '../../api';
import { Button, Empty, Notice, PageHead, Panel, QueryState, useUi } from '../../ui';

export function Cafes() {
  const q = useQuery({ queryKey: ['cafes'], queryFn: async () => list<Cafe>((await api.get<Page<Cafe>>('/api/cafes', { params: { size: 50 } })).data) });
  return <>
    <PageHead title="Find your perfect spot" sub="Explore welcoming cafés and productive study spaces." />
    <QueryState loading={q.isLoading} error={q.error} />
    <div className="card-grid">{q.data?.map(c => <Link className="item-card cafe-card" key={c.id} to={'/cafes/' + c.id}>
      <div className="cafe-art"><Coffee size={43} /></div><h3>{c.name}</h3><p>{c.description || 'A cozy place to work and connect.'}</p>
      <div className="meta"><MapPin size={15} />{c.location}</div><div className="card-footer">Explore spaces <ArrowRight size={16} /></div></Link>)}</div>
    {q.data?.length === 0 && <Empty message="No cafés available." />}
  </>;
}

export function CafeDetails() {
  const { id } = useParams(); const { toast } = useUi(); const qc = useQueryClient();
  const cafe = useQuery({ queryKey: ['cafe', id], queryFn: async () => (await api.get<Cafe>('/api/cafes/' + id)).data });
  const spaces = useQuery({ queryKey: ['spaces', id], queryFn: async () => list<Space>((await api.get<Page<Space>>('/api/study-spaces', { params: { cafeId: id, size: 100 } })).data) });
  const [chosen, setChosen] = useState<number | null>(null); const [start, setStart] = useState(''); const [end, setEnd] = useState('');
  const m = useMutation({
    mutationFn: () => api.post('/api/bookings', { studySpaceId: chosen, startTime: start, endTime: end }),
    onSuccess: () => { toast('Booking requested. The café will confirm it soon.'); qc.invalidateQueries({ queryKey: ['bookings'] }); setStart(''); setEnd(''); },
  });
  const space = spaces.data?.find(s => s.id === chosen);
  return <>
    <Link className="back" to="/cafes">← All cafés</Link>
    <QueryState loading={cafe.isLoading} error={cafe.error} />
    {cafe.data && <><PageHead title={cafe.data.name} sub={cafe.data.location} /><p className="lead">{cafe.data.description}</p></>}
    <div className="detail-layout">
      <Panel title="Available study spaces">
        <QueryState loading={spaces.isLoading} error={spaces.error} />
        {spaces.data?.length ? spaces.data.map(s => <button key={s.id} onClick={() => setChosen(s.id)} className={'space-choice ' + (chosen === s.id ? 'selected' : '')}>
          <strong>{s.name}</strong>
          <span><UsersRound size={14} /> {s.capacity} seat{s.capacity === 1 ? '' : 's'} · {s.pricePerHour ?? '—'} BHD/hour</span>
          <span>{s.capacity >= 2 ? 'Group space — you can host a study session here' : 'Single seat — study sessions need 2 or more seats'}</span>
        </button>) : !spaces.isLoading && <Empty message="No study spaces available in this café." />}
      </Panel>
      <Panel title="Reserve a space">
        <label className="field">Start date and time<input type="datetime-local" value={start} onChange={e => setStart(e.target.value)} /></label>
        <label className="field">End date and time<input type="datetime-local" value={end} onChange={e => setEnd(e.target.value)} /></label>
        {space && <p className="muted">Selected: {space.name} ({space.capacity} seats)</p>}
        <Notice text={m.error ? errorText(m.error) : ''} />
        <Button disabled={!chosen || !start || !end || end <= start} busy={m.isPending} onClick={() => m.mutate()}>Request booking</Button>
        <p className="muted">Bookings stay pending until the café manager confirms them.</p>
      </Panel>
    </div>
  </>;
}

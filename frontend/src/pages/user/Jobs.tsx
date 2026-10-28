import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowRight, BriefcaseBusiness, Clock, MapPin, Search, Users, Wallet } from 'lucide-react';
import { api, errorText, fmt, list, type Job, type Page } from '../../api';
import { Button, Empty, Notice, PageHead, Panel, QueryState, StatusBadge, useUi } from '../../ui';

const TYPES = ['ALL', 'FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP', 'FREELANCE'];

function daysLeft(deadline?: string) {
  if (!deadline) return null;
  const d = Math.ceil((new Date(deadline).getTime() - Date.now()) / 86400000);
  return d < 0 ? 'Closed' : d === 0 ? 'Closes today' : `${d} day${d === 1 ? '' : 's'} left`;
}

export const left = (j: Job) => { const n = (j.vacancies ?? 1) - (j.filledVacancies ?? 0); return n <= 0 ? 'All positions filled' : `${n} of ${j.vacancies ?? 1} position${(j.vacancies ?? 1) === 1 ? '' : 's'} open`; };

export function Jobs() {
  const [search, setSearch] = useState(''); const [type, setType] = useState('ALL');
  const q = useQuery({ queryKey: ['jobs', search, type], queryFn: async () => list<Job>((await api.get<Page<Job>>('/api/jobs', { params: { keyword: search || undefined, jobType: type === 'ALL' ? undefined : type, status: 'OPEN', size: 50 } })).data) });
  return <>
    <PageHead title="Job offers" sub="Open roles from approved employers. Apply in a minute." />
    <div className="toolbar"><Search size={19} /><input placeholder="Search by title, company or skill…" value={search} onChange={e => setSearch(e.target.value)} /></div>
    <div className="chips">{TYPES.map(t => <button key={t} className={'chip ' + (type === t ? 'on' : '')} onClick={() => setType(t)}>{t.replaceAll('_', ' ')}</button>)}</div>
    <QueryState loading={q.isLoading} error={q.error} />
    <div className="card-grid">{q.data?.map(j => <Link to={'/jobs/' + j.id} className="item-card" key={j.id}>
      <div className="card-icon"><BriefcaseBusiness /></div>
      <StatusBadge value={j.jobType} />
      <h3>{j.title}</h3><p>{j.companyName}</p>
      <div className="meta"><MapPin size={15} />{j.location}</div>
      <div className="meta"><Wallet size={15} />{j.salary != null ? `${j.salary} BHD / month` : 'Salary not disclosed'}</div>
      <div className="meta"><Clock size={15} />{daysLeft(j.deadline)}</div>
      <div className="meta"><Users size={15} />{left(j)}</div>
      <div className="card-footer">View offer <ArrowRight size={16} /></div></Link>)}</div>
    {q.data?.length === 0 && <Empty message="No open job offers match your search." />}
  </>;
}

export function JobDetails() {
  const { id } = useParams(); const { toast } = useUi(); const qc = useQueryClient();
  const q = useQuery({ queryKey: ['job', id], queryFn: async () => (await api.get<Job>('/api/jobs/' + id)).data });
  const [cover, setCover] = useState('');
  const m = useMutation({
    mutationFn: () => api.post('/api/applications/jobs/' + id, { coverLetter: cover }),
    onSuccess: () => { toast('Application submitted. Good luck!'); qc.invalidateQueries({ queryKey: ['applications'] }); setCover(''); },
  });
  const closed = q.data && (q.data.status !== 'OPEN' || (q.data.deadline && new Date(q.data.deadline) < new Date()));
  return <>
    <Link className="back" to="/jobs">← All job offers</Link>
    <QueryState loading={q.isLoading} error={q.error} />
    {q.data && <div className="detail-layout">
      <Panel title={q.data.title}>
        <p className="lead">{q.data.companyName} · {q.data.location}</p>
        <div className="chips static"><StatusBadge value={q.data.jobType} /><StatusBadge value={q.data.status} /></div>
        <div className="facts">
          <div><span>Salary</span><strong>{q.data.salary != null ? `${q.data.salary} BHD / month` : 'Not disclosed'}</strong></div>
          <div><span>Apply before</span><strong>{fmt(q.data.deadline)}</strong></div>
          <div><span>Open positions</span><strong>{left(q.data)}</strong></div>
          <div><span>Posted by</span><strong>{q.data.createdBy || '—'}</strong></div>
        </div>
        <h3>About this role</h3><p className="preline">{q.data.description}</p>
      </Panel>
      <Panel title="Apply for this role">
        {closed ? <Notice text="This offer is no longer accepting applications." /> : <>
          <label className="field">Cover letter (optional)<textarea rows={6} maxLength={1000} value={cover} onChange={e => setCover(e.target.value)} /></label>
          <Notice text={m.error ? errorText(m.error) : ''} />
          <Button busy={m.isPending} onClick={() => m.mutate()}>Submit application</Button>
          <p className="muted">You can follow the decision under “My applications”.</p></>}
      </Panel>
    </div>}
  </>;
}

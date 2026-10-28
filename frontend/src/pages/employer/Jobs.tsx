import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BriefcaseBusiness, Pencil, Plus, Trash2 } from 'lucide-react';
import { api, errorText, fmt, type Application, type Job } from '../../api';
import { Button, Empty, Notice, PageHead, Panel, QueryState, StatusBadge, useUi } from '../../ui';

const TYPES = ['FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERNSHIP', 'FREELANCE'];
const blank = { title: '', description: '', companyName: '', location: '', jobType: 'FULL_TIME', salary: '', deadline: '', vacancies: '1' };

function JobForm({ job, onDone }: { job?: Job; onDone: () => void }) {
  const { toast } = useUi(); const qc = useQueryClient();
  const [f, setF] = useState(job ? { title: job.title, description: job.description, companyName: job.companyName, location: job.location, jobType: job.jobType, salary: job.salary != null ? String(job.salary) : '', deadline: (job.deadline || '').slice(0, 16), vacancies: String(job.vacancies ?? 1) } : blank);
  const m = useMutation({
    mutationFn: () => { const body = { ...f, salary: f.salary === '' ? null : Number(f.salary), vacancies: Number(f.vacancies || 1) }; return job ? api.put('/api/jobs/' + job.id, body) : api.post('/api/jobs', body); },
    onSuccess: () => { toast(job ? 'Job updated.' : 'Job published.'); qc.invalidateQueries({ queryKey: ['my-jobs'] }); onDone(); },
  });
  const t = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) => setF(v => ({ ...v, [k]: e.target.value }));
  return <Panel title={job ? 'Edit job' : 'Post a job offer'} action={<button className="text-btn" onClick={onDone}>Close</button>}>
    <form className="form-grid" onSubmit={e => { e.preventDefault(); m.mutate(); }}>
      <label className="field">Job title<input required maxLength={150} value={f.title} onChange={t('title')} /></label>
      <label className="field">Company<input required maxLength={150} value={f.companyName} onChange={t('companyName')} /></label>
      <label className="field">Location<input required maxLength={150} value={f.location} onChange={t('location')} /></label>
      <label className="field">Monthly salary (BHD)<input type="number" min={0} value={f.salary} onChange={t('salary')} /></label>
      <label className="field">Job type<select value={f.jobType} onChange={t('jobType')}>{TYPES.map(x => <option key={x}>{x}</option>)}</select></label>
      <label className="field">Open positions (vacancies)<input required type="number" min={Math.max(1, job?.filledVacancies ?? 1)} max={1000} value={f.vacancies} onChange={t('vacancies')} /></label>
      <label className="field">Apply before<input required type="datetime-local" value={f.deadline} onChange={t('deadline')} /></label>
      <label className="field full">Description<textarea required maxLength={2000} rows={4} value={f.description} onChange={t('description')} /></label>
      <div className="full"><Notice text={m.error ? errorText(m.error) : ''} /><Button busy={m.isPending} type="submit">{job ? 'Save changes' : 'Publish job'}</Button></div>
    </form></Panel>;
}

function Applicants({ job }: { job: Job }) {
  const { toast } = useUi(); const qc = useQueryClient();
  const q = useQuery({ queryKey: ['job-applicants', job.id], queryFn: async () => (await api.get<Application[]>(`/api/applications/jobs/${job.id}`)).data });
  const m = useMutation({ mutationFn: ({ id, status }: { id: number; status: string }) => api.put(`/api/applications/${id}/status`, null, { params: { status } }),
    onSuccess: (_r, v) => { toast(`Application ${v.status.toLowerCase()}.`); qc.invalidateQueries({ queryKey: ['job-applicants', job.id] }); qc.invalidateQueries({ queryKey: ['my-jobs'] }); } });
  return <Panel title={`Applicants · ${job.title}`} action={<span className="status">{job.filledVacancies ?? 0}/{job.vacancies ?? 1} filled</span>}>
    <QueryState loading={q.isLoading} error={q.error} /><Notice text={m.error ? errorText(m.error) : ''} />
    <div className="list-stack">{q.data?.map(a => <div className="applicant" key={a.id}>
      <div className="list-row flat"><div className="list-main"><strong>{a.applicantUsername}</strong><span>Applied {fmt(a.appliedAt)}</span></div><StatusBadge value={a.status} />
        {a.status === 'PENDING' && <><Button busy={m.isPending} disabled={(job.filledVacancies ?? 0) >= (job.vacancies ?? 1)} title={(job.filledVacancies ?? 0) >= (job.vacancies ?? 1) ? 'All positions are filled' : undefined} onClick={() => m.mutate({ id: a.id, status: 'ACCEPTED' })}>Accept</Button><button className="text-btn danger" onClick={() => m.mutate({ id: a.id, status: 'REJECTED' })}>Reject</button></>}</div>
      {a.coverLetter && <blockquote>{a.coverLetter}</blockquote>}</div>)}</div>
    {q.data?.length === 0 && <Empty message="Nobody has applied yet." />}
  </Panel>;
}

export default function EmployerJobs() {
  const { toast, confirm } = useUi(); const qc = useQueryClient(); const [params, setParams] = useSearchParams();
  const [form, setForm] = useState<'new' | Job | null>(null);
  const q = useQuery({ queryKey: ['my-jobs'], queryFn: async () => (await api.get<Job[]>('/api/jobs/my')).data });
  const selected = q.data?.find(j => String(j.id) === params.get('job'));
  const del = useMutation({ mutationFn: (id: number) => api.delete('/api/jobs/' + id), onSuccess: () => { toast('Job closed.'); qc.invalidateQueries({ queryKey: ['my-jobs'] }); setParams({}); } });
  return <>
    <PageHead eyebrow="EMPLOYER AREA" title="My jobs & applicants" sub="Post offers, then accept or reject the people who apply." />
    <div className="page-actions"><Button onClick={() => setForm('new')}><Plus size={17} /> Post a job</Button></div>
    {form && <JobForm job={form === 'new' ? undefined : form} onDone={() => setForm(null)} />}
    <Notice text={del.error ? errorText(del.error) : ''} />
    <QueryState loading={q.isLoading} error={q.error} />
    <div className="list-stack">{q.data?.map(j => <div className={'list-row ' + (selected?.id === j.id ? 'picked' : '')} key={j.id}>
      <div className="card-icon"><BriefcaseBusiness /></div>
      <div className="list-main"><strong>{j.title}</strong><span>{j.location} · {j.salary != null ? `${j.salary} BHD` : 'no salary'} · closes {fmt(j.deadline)} · <b>{j.filledVacancies ?? 0}/{j.vacancies ?? 1}</b> positions filled</span></div>
      <StatusBadge value={j.status} />
      <button className="button small" onClick={() => setParams({ job: String(j.id) })}>Applicants</button>
      {j.status === 'OPEN' && <button className="icon-btn" title="Edit" onClick={() => setForm(j)}><Pencil size={17} /></button>}
      {j.status === 'OPEN' && <button className="icon-btn" title="Close job" onClick={async () => { if (await confirm(`Close “${j.title}”? It will stop accepting applications.`)) del.mutate(j.id); }}><Trash2 size={17} /></button>}</div>)}</div>
    {q.data?.length === 0 && <Empty message="You haven't posted a job yet." />}
    {selected && <Applicants job={selected} />}
  </>;
}

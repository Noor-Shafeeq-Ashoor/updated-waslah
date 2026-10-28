import { Link } from 'react-router-dom';
import { useQueries, useQuery } from '@tanstack/react-query';
import { api, type Application, type Job } from '../../api';
import { useAuth } from '../../auth';
import { PageHead, Panel, QueryState, StatCard } from '../../ui';

export default function EmployerOverview() {
  const { username } = useAuth();
  const jobs = useQuery({ queryKey: ['my-jobs'], queryFn: async () => (await api.get<Job[]>('/api/jobs/my')).data });
  const apps = useQueries({ queries: (jobs.data ?? []).map(j => ({ queryKey: ['job-applicants', j.id], queryFn: async () => (await api.get<Application[]>(`/api/applications/jobs/${j.id}`)).data })) });
  const all = apps.flatMap(a => a.data ?? []);
  const pending = all.filter(a => a.status === 'PENDING');
  return <>
    <PageHead eyebrow="EMPLOYER AREA" title={`Welcome, ${username}.`} sub="Your jobs and the people who applied to them." />
    <QueryState loading={jobs.isLoading} error={jobs.error} />
    <div className="stat-grid">
      <StatCard label="Jobs posted" value={jobs.data?.length ?? '–'} />
      <StatCard label="Open jobs" value={jobs.data?.filter(j => j.status === 'OPEN').length ?? '–'} />
      <StatCard label="Applicants to review" value={pending.length} tone={pending.length ? 'alert' : undefined} hint={`${all.length} in total`} />
    </div>
    <Panel title="Latest applicants" action={<Link className="text-btn" to="/employer/jobs">Open jobs →</Link>}>
      <div className="list-stack">{pending.slice(0, 5).map(a => <Link to={'/employer/jobs?job=' + a.jobId} className="list-row" key={a.id}><div className="list-main"><strong>{a.applicantUsername}</strong><span>applied for {a.jobTitle}</span></div>→</Link>)}</div>
      {pending.length === 0 && <p className="muted">No applications are waiting for a decision.</p>}
    </Panel>
  </>;
}

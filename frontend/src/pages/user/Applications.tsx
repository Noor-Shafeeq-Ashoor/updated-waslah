import { useQuery } from '@tanstack/react-query';
import { BriefcaseBusiness } from 'lucide-react';
import { Link } from 'react-router-dom';
import { api, fmt, type Application } from '../../api';
import { Empty, PageHead, QueryState, StatusBadge } from '../../ui';

export default function Applications() {
  const q = useQuery({ queryKey: ['applications'], queryFn: async () => (await api.get<Application[]>('/api/applications/my')).data });
  return <>
    <PageHead title="My applications" sub="Keep track of every opportunity you applied for." />
    <QueryState loading={q.isLoading} error={q.error} />
    <div className="list-stack">{q.data?.map(a => <div className="list-row" key={a.id}><div className="card-icon"><BriefcaseBusiness /></div>
      <div className="list-main"><strong>{a.jobTitle || `Job #${a.jobId}`}</strong><span>Applied {fmt(a.appliedAt)}</span></div>
      <StatusBadge value={a.status} /><Link className="text-btn" to={'/jobs/' + a.jobId}>View offer</Link></div>)}</div>
    {q.data?.length === 0 && <Empty message="You haven't applied for any jobs yet." />}
  </>;
}

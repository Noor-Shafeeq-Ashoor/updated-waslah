import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, Store } from 'lucide-react';
import { api, errorText, fmt, list, type CafeRequest, type EmployerRequest } from '../../api';
import { Button, Empty, Notice, PageHead, QueryState, StatusBadge, useUi } from '../../ui';

export default function Requests() {
  const { toast, confirm } = useUi(); const qc = useQueryClient();
  const [tab, setTab] = useState<'employer' | 'cafe'>('employer'); const [status, setStatus] = useState('PENDING');
  const emp = useQuery({ queryKey: ['admin-employer-requests', status], queryFn: async () => list<EmployerRequest>((await api.get('/api/admin/employer-requests', { params: { status } })).data) });
  const cafe = useQuery({ queryKey: ['admin-cafe-requests', status], queryFn: async () => list<CafeRequest>((await api.get('/api/cafe-requests', { params: { status } })).data) });
  const done = (msg: string) => { toast(msg); qc.invalidateQueries({ queryKey: ['admin-employer-requests'] }); qc.invalidateQueries({ queryKey: ['admin-cafe-requests'] }); qc.invalidateQueries({ queryKey: ['admin-stats'] }); };
  const decideE = useMutation({ mutationFn: ({ id, action }: { id: number; action: 'approve' | 'reject' }) => api.put(`/api/admin/employer-requests/${id}/${action}`), onSuccess: (_r, v) => done(`Request ${v.action}d.`) });
  const decideC = useMutation({ mutationFn: ({ id, decision }: { id: number; decision: 'APPROVED' | 'REJECTED' }) => api.put(`/api/cafe-requests/${id}/decision`, { decision }), onSuccess: (_r, v) => done(`Request ${v.decision.toLowerCase()}.`) });
  const err = decideE.error || decideC.error;
  const q = tab === 'employer' ? emp : cafe;
  return <>
    <PageHead eyebrow="ADMINISTRATION" title="Requests" sub="Approve who may post jobs and who may run a café on WASLAH." />
    <div className="chips"><button className={'chip ' + (tab === 'employer' ? 'on' : '')} onClick={() => setTab('employer')}><Building2 size={14} /> Employers</button>
      <button className={'chip ' + (tab === 'cafe' ? 'on' : '')} onClick={() => setTab('cafe')}><Store size={14} /> Cafés</button><span className="spacer" />
      {['PENDING', 'APPROVED', 'REJECTED', 'ALL'].map(s => <button key={s} className={'chip ' + (status === s ? 'on' : '')} onClick={() => setStatus(s)}>{s}</button>)}</div>
    <Notice text={err ? errorText(err) : ''} />
    <QueryState loading={q.isLoading} error={q.error} />
    <div className="list-stack">
      {tab === 'employer' && emp.data?.map(r => <div className="list-row" key={r.id}><div className="card-icon"><Building2 /></div>
        <div className="list-main"><strong>{r.companyName || 'Employer request'}</strong><span>{r.username} · CR {r.commercialRegistration} · {r.jobTypes}</span>{r.companyDescription && <span>{r.companyDescription}</span>}</div>
        <StatusBadge value={r.status} />{r.status === 'PENDING' && <><Button busy={decideE.isPending} onClick={() => decideE.mutate({ id: r.id, action: 'approve' })}>Approve</Button>
          <button className="text-btn danger" onClick={async () => { if (await confirm('Reject this employer request?')) decideE.mutate({ id: r.id, action: 'reject' }); }}>Reject</button></>}</div>)}
      {tab === 'cafe' && cafe.data?.map(r => <div className="list-row" key={r.id}><div className="card-icon"><Store /></div>
        <div className="list-main"><strong>{r.cafeName}</strong><span>{r.requestedByUsername} · {r.location} · {fmt(r.createdAt)}</span><span>{r.description}</span></div>
        <StatusBadge value={r.status} />{r.status === 'PENDING' && <><Button busy={decideC.isPending} onClick={() => decideC.mutate({ id: r.id, decision: 'APPROVED' })}>Approve</Button>
          <button className="text-btn danger" onClick={async () => { if (await confirm('Reject this café request?')) decideC.mutate({ id: r.id, decision: 'REJECTED' }); }}>Reject</button></>}</div>)}
    </div>
    {q.data?.length === 0 && <Empty message="No requests with this status." />}
  </>;
}

import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api, errorText, list, type CafeRequest, type EmployerRequest } from '../../api';
import { Button, Notice, PageHead, Panel, StatusBadge, useUi } from '../../ui';

export default function Partner() {
  const { toast } = useUi(); const qc = useQueryClient();
  const emp = useQuery({ queryKey: ['my-employer-request'], retry: false, queryFn: async () => { try { return (await api.get<EmployerRequest>('/api/employer-requests/my')).data; } catch { return null; } } });
  const cafes = useQuery({ queryKey: ['my-cafe-requests'], retry: false, queryFn: async () => { try { return list<CafeRequest>((await api.get('/api/cafe-requests/my')).data); } catch { return []; } } });
  const [e, setE] = useState({ companyName: '', companyDescription: '', jobTypes: 'FULL_TIME', commercialRegistration: '' });
  const [c, setC] = useState({ cafeName: '', description: '', location: '' });
  const sendE = useMutation({ mutationFn: () => api.post('/api/employer-requests', e), onSuccess: () => { toast('Request sent. An admin will review it.'); qc.invalidateQueries({ queryKey: ['my-employer-request'] }); } });
  const sendC = useMutation({ mutationFn: () => api.post('/api/cafe-requests', c), onSuccess: () => { toast('Request sent. An admin will review it.'); qc.invalidateQueries({ queryKey: ['my-cafe-requests'] }); } });
  return <>
    <PageHead title="Partner with us" sub="Hire talent as an employer, or list your café and workspaces. An admin reviews every request." />
    <div className="detail-layout">
      <Panel title="Become an employer" action={emp.data ? <StatusBadge value={emp.data.status} /> : undefined}>
        <form className="form-stack" onSubmit={ev => { ev.preventDefault(); sendE.mutate(); }}>
          <label className="field">Company name<input required maxLength={150} value={e.companyName} onChange={x => setE({ ...e, companyName: x.target.value })} /></label>
          <label className="field">About the company<textarea required maxLength={1000} rows={3} value={e.companyDescription} onChange={x => setE({ ...e, companyDescription: x.target.value })} /></label>
          <label className="field">Job types you will post<input required value={e.jobTypes} onChange={x => setE({ ...e, jobTypes: x.target.value })} /></label>
          <label className="field">Commercial registration<input required value={e.commercialRegistration} onChange={x => setE({ ...e, commercialRegistration: x.target.value })} /></label>
          <Notice text={sendE.error ? errorText(sendE.error) : ''} /><Button busy={sendE.isPending} type="submit">Send request</Button></form></Panel>
      <Panel title="Register a café">
        {cafes.data?.map(r => <div className="list-row" key={r.id}><div className="list-main"><strong>{r.cafeName}</strong><span>{r.location}</span></div><StatusBadge value={r.status} /></div>)}
        <form className="form-stack" onSubmit={ev => { ev.preventDefault(); sendC.mutate(); }}>
          <label className="field">Café name<input required maxLength={55} value={c.cafeName} onChange={x => setC({ ...c, cafeName: x.target.value })} /></label>
          <label className="field">Description<textarea required maxLength={1000} rows={3} value={c.description} onChange={x => setC({ ...c, description: x.target.value })} /></label>
          <label className="field">Location<input required value={c.location} onChange={x => setC({ ...c, location: x.target.value })} /></label>
          <Notice text={sendC.error ? errorText(sendC.error) : ''} /><Button busy={sendC.isPending} type="submit">Send request</Button></form></Panel>
    </div>
    <p className="muted">After approval, sign in again to see your new area.</p>
  </>;
}

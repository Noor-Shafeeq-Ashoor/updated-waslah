import { useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { api, fmt, type AuditLog } from '../../api';
import { Empty, PageHead, QueryState } from '../../ui';

type Spring = { content: AuditLog[]; totalPages: number };
export default function Audit() {
  const [page, setPage] = useState(0);
  const q = useQuery({ queryKey: ['audit', page], placeholderData: keepPreviousData, queryFn: async () => (await api.get<Spring>('/api/admin/audit-logs', { params: { page, size: 15, sort: 'timestamp,desc' } })).data });
  return <>
    <PageHead eyebrow="ADMINISTRATION" title="Audit log" sub="A permanent record of important actions on the platform." />
    <QueryState loading={q.isLoading} error={q.error} />
    <div className="table-wrap"><table className="table"><thead><tr><th>When</th><th>Who</th><th>Action</th><th>Target</th></tr></thead><tbody>
      {q.data?.content.map(l => <tr key={l.id}><td>{fmt(l.timestamp)}</td><td>{l.actor || 'system'}</td><td><b>{l.action.replaceAll('_', ' ')}</b></td><td>{l.targetType} #{l.targetId}</td></tr>)}</tbody></table></div>
    {q.data?.content.length === 0 && <Empty message="No activity recorded yet." />}
    {q.data && q.data.totalPages > 1 && <div className="pager"><button className="text-btn" disabled={page === 0} onClick={() => setPage(page - 1)}>← Newer</button><span>Page {page + 1} of {q.data.totalPages}</span><button className="text-btn" disabled={page + 1 >= q.data.totalPages} onClick={() => setPage(page + 1)}>Older →</button></div>}
  </>;
}

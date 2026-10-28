import { useState } from 'react';
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Search } from 'lucide-react';
import { api, errorText, type AdminUser, type Page } from '../../api';
import { useAuth } from '../../auth';
import { Avatar, Empty, Notice, PageHead, QueryState, StatusBadge, useUi } from '../../ui';

export default function Users() {
  const { toast, confirm } = useUi(); const qc = useQueryClient(); const me = useAuth().username;
  const [keyword, setKeyword] = useState(''); const [role, setRole] = useState(''); const [status, setStatus] = useState(''); const [page, setPage] = useState(0);
  const q = useQuery({
    queryKey: ['admin-users', keyword, role, status, page], placeholderData: keepPreviousData,
    queryFn: async () => (await api.get<Page<AdminUser>>('/api/admin/users', { params: { keyword: keyword || undefined, role: role || undefined, status: status || undefined, page, size: 10, sort: 'id', direction: 'desc' } })).data,
  });
  const m = useMutation({
    mutationFn: ({ u, action }: { u: AdminUser; action: 'deactivate' | 'reactivate' }) => action === 'deactivate' ? api.delete(`/api/admin/users/${u.id}`) : api.put(`/api/admin/users/${u.id}/reactivate`),
    onSuccess: (_r, v) => { toast(v.action === 'deactivate' ? `${v.u.username} deactivated.` : `${v.u.username} reactivated.`); qc.invalidateQueries({ queryKey: ['admin-users'] }); qc.invalidateQueries({ queryKey: ['admin-stats'] }); },
  });
  const sel = (v: string, set: (x: string) => void, opts: string[], label: string) =>
    <select aria-label={label} value={v} onChange={e => { set(e.target.value); setPage(0); }}><option value="">{label}</option>{opts.map(o => <option key={o}>{o}</option>)}</select>;
  return <>
    <PageHead eyebrow="ADMINISTRATION" title="Users" sub="Search accounts, deactivate access, or bring someone back." />
    <div className="filters"><div className="toolbar grow"><Search size={19} /><input placeholder="Search username or email…" value={keyword} onChange={e => { setKeyword(e.target.value); setPage(0); }} /></div>
      {sel(role, setRole, ['USER', 'EMPLOYER', 'CAFE_MANAGER', 'ADMIN'], 'All roles')}{sel(status, setStatus, ['ACTIVE', 'INACTIVE'], 'All statuses')}</div>
    <Notice text={m.error ? errorText(m.error) : ''} />
    <QueryState loading={q.isLoading} error={q.error} />
    <div className="table-wrap"><table className="table"><thead><tr><th>User</th><th>Email</th><th>Role</th><th>Status</th><th /></tr></thead><tbody>
      {q.data?.content.map(u => <tr key={u.id}>
        <td><div className="cell-user"><Avatar name={u.username} size={32} /><div><strong>{u.username}</strong><small>{[u.firstName, u.lastName].filter(Boolean).join(' ') || '—'}</small></div></div></td>
        <td>{u.email}{!u.emailVerified && <small className="warn"> · not verified</small>}</td>
        <td>{u.role.replace('_', ' ')}</td><td><StatusBadge value={u.status} /></td>
        <td className="right">{u.username === me ? <span className="muted tiny">You</span> : u.status === 'ACTIVE'
          ? <button className="text-btn danger" onClick={async () => { if (await confirm(`Deactivate ${u.username}? They will no longer be able to sign in.`)) m.mutate({ u, action: 'deactivate' }); }}>Deactivate</button>
          : <button className="text-btn" onClick={() => m.mutate({ u, action: 'reactivate' })}>Reactivate</button>}</td></tr>)}
    </tbody></table></div>
    {q.data?.content.length === 0 && <Empty message="No users match these filters." />}
    {q.data && q.data.totalPages > 1 && <div className="pager"><button className="text-btn" disabled={page === 0} onClick={() => setPage(page - 1)}>← Previous</button><span>Page {page + 1} of {q.data.totalPages}</span><button className="text-btn" disabled={page + 1 >= q.data.totalPages} onClick={() => setPage(page + 1)}>Next →</button></div>}
  </>;
}

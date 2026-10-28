import { useEffect, useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Camera } from 'lucide-react';
import { api, errorText, type Profile as P } from '../api';
import { useAuth } from '../auth';
import { Avatar, Button, Notice, PageHead, Panel, QueryState, useUi } from '../ui';
import { useProfile, useProfileImage } from '../hooks';

export default function Profile() {
  const { username, role } = useAuth(); const { toast } = useUi(); const qc = useQueryClient();
  const q = useProfile(); const img = useProfileImage();
  const [form, setForm] = useState<P>({}); const file = useRef<HTMLInputElement>(null); const [imgErr, setImgErr] = useState('');
  useEffect(() => { if (q.data) setForm(q.data); }, [q.data]);
  const save = useMutation({
    mutationFn: () => api.put('/api/profile', { firstName: form.firstName, lastName: form.lastName, phone: form.phone || null, bio: form.bio || null, location: form.location || null }),
    onSuccess: () => { toast('Profile updated.'); qc.invalidateQueries({ queryKey: ['profile'] }); },
  });
  const upload = useMutation({
    mutationFn: (f: File) => { const fd = new FormData(); fd.append('image', f); return api.put('/api/profile/image', fd); },
    onSuccess: () => { toast('Profile photo updated.'); qc.invalidateQueries({ queryKey: ['profile-image'] }); qc.invalidateQueries({ queryKey: ['profile'] }); },
  });
  function pick(f?: File) {
    setImgErr(''); if (!f) return;
    if (!['image/jpeg', 'image/png'].includes(f.type)) return setImgErr('Please choose a JPG or PNG image.');
    if (f.size > 2 * 1024 * 1024) return setImgErr('The image must be 2 MB or smaller.');
    upload.mutate(f);
  }
  const set = (k: keyof P) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => setForm(v => ({ ...v, [k]: e.target.value }));
  return <>
    <PageHead title="My profile" sub="Make your WASLAH space feel like you." />
    <div className="profile-banner">
      <button className="avatar-edit" onClick={() => file.current?.click()} aria-label="Change photo"><Avatar name={username || '?'} src={img.data} size={84} /><span><Camera size={16} /></span></button>
      <input ref={file} type="file" accept="image/png,image/jpeg" hidden onChange={e => { pick(e.target.files?.[0]); e.target.value = ''; }} />
      <div><h2>{form.firstName ? `${form.firstName} ${form.lastName || ''}` : username}</h2><p>{role.replace('_', ' ')} · WASLAH member</p>
        <button className="text-btn" onClick={() => file.current?.click()}>{upload.isPending ? 'Uploading…' : 'Change photo'}</button></div>
    </div>
    <Notice text={imgErr || (upload.error ? errorText(upload.error) : '')} />
    <Panel title="Personal details"><QueryState loading={q.isLoading} error={q.error} />
      <form className="form-grid" onSubmit={e => { e.preventDefault(); save.mutate(); }}>
        <label className="field">First name<input required maxLength={50} value={form.firstName || ''} onChange={set('firstName')} /></label>
        <label className="field">Last name<input required maxLength={50} value={form.lastName || ''} onChange={set('lastName')} /></label>
        <label className="field">Phone (8 digits)<input pattern="[0-9]{8}" value={form.phone || ''} onChange={set('phone')} /></label>
        <label className="field">Location<input maxLength={100} value={form.location || ''} onChange={set('location')} /></label>
        <label className="field full">About me<textarea rows={4} maxLength={500} value={form.bio || ''} onChange={set('bio')} /></label>
        <div className="full"><Notice text={save.error ? errorText(save.error) : ''} /><Button busy={save.isPending} type="submit">Save changes</Button></div>
      </form></Panel>
  </>;
}

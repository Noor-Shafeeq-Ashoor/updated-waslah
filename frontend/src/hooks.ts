import { useEffect } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { api, type Profile } from './api';
import { useAuth } from './auth';

/** The profile photo needs the JWT, so it is fetched as a blob instead of an <img src>. */
export function useProfileImage() {
  return useQuery({
    queryKey: ['profile-image'],
    staleTime: 5 * 60_000,
    retry: false,
    queryFn: async () => {
      try {
        const r = await api.get('/api/profile/image', { responseType: 'blob' });
        return URL.createObjectURL(r.data);
      } catch { return null; }
    },
  });
}

export function useProfile() {
  return useQuery({
    queryKey: ['profile'],
    retry: false,
    queryFn: async () => {
      try { return (await api.get<Profile>('/api/profile')).data; }
      catch { return {} as Profile; } // 404 until the profile is first saved
    },
  });
}

/**
 * Listens to the SSE stream with fetch (EventSource cannot send the Authorization header)
 * and calls onEvent for every event. Reconnects after a short pause.
 */
export function useNotificationStream(onEvent: (name: string, data: string) => void) {
  const { token } = useAuth();
  const qc = useQueryClient();
  useEffect(() => {
    if (!token) return;
    const ctrl = new AbortController();
    let stopped = false;
    async function run() {
      while (!stopped) {
        try {
          const res = await fetch((import.meta.env.VITE_API_URL || '') + '/api/notifications/stream', {
            headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' }, signal: ctrl.signal,
          });
          if (!res.ok || !res.body) throw new Error('stream');
          const reader = res.body.getReader(); const dec = new TextDecoder(); let buf = '';
          for (;;) {
            const { done, value } = await reader.read(); if (done) break;
            buf += dec.decode(value, { stream: true });
            let i;
            while ((i = buf.indexOf('\n\n')) >= 0) {
              const block = buf.slice(0, i); buf = buf.slice(i + 2);
              let name = 'message', data = '';
              block.split('\n').forEach(l => { if (l.startsWith('event:')) name = l.slice(6).trim(); else if (l.startsWith('data:')) data += l.slice(5).trim(); });
              if (data || name !== 'message') { onEvent(name, data); qc.invalidateQueries({ queryKey: ['notifications'] }); qc.invalidateQueries({ queryKey: ['sessions'] }); qc.invalidateQueries({ queryKey: ['bookings'] }); }
            }
          }
        } catch { /* fall through to retry */ }
        if (!stopped) await new Promise(r => setTimeout(r, 5000));
      }
    }
    run();
    return () => { stopped = true; ctrl.abort(); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token]);
}

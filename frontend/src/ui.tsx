import React, { createContext, useCallback, useContext, useState } from 'react';
import { AlertCircle, CheckCircle2, Coffee } from 'lucide-react';
import { errorText } from './api';

export function Notice({ text, kind = 'error' }: { text?: string; kind?: 'error' | 'success' }) {
  return text ? <div className={'notice ' + kind} role="status">{kind === 'error' ? <AlertCircle size={17} /> : <CheckCircle2 size={17} />} {text}</div> : null;
}

export function Button({ children, busy = false, ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { busy?: boolean }) {
  return <button {...props} disabled={busy || props.disabled} className={'button ' + (props.className || '')}>{busy ? 'Working…' : children}</button>;
}

export function Panel({ title, children, action }: { title: string; children: React.ReactNode; action?: React.ReactNode }) {
  return <section className="panel"><div className="panel-heading"><h2>{title}</h2>{action}</div>{children}</section>;
}

export function Empty({ message = 'Nothing here yet.' }: { message?: string }) {
  return <div className="empty"><Coffee size={27} /><p>{message}</p></div>;
}

export function QueryState({ loading, error }: { loading: boolean; error: unknown }) {
  return loading ? <div className="skeleton-stack"><div className="skeleton" /><div className="skeleton" /><div className="skeleton short" /></div>
    : error ? <Notice text={errorText(error)} /> : null;
}

export function PageHead({ title, sub, eyebrow = 'WASLAH · CONNECT & GROW' }: { title: string; sub: string; eyebrow?: string }) {
  return <div className="page-head"><span className="eyebrow">{eyebrow}</span><h1>{title}</h1><p>{sub}</p></div>;
}

const WARN = ['PENDING', 'FULL', 'WAITLISTED', 'INACTIVE', 'REJECTED', 'CANCELLED', 'CLOSED', 'HIDDEN'];
export function StatusBadge({ value }: { value: string }) {
  return <span className={'status ' + (WARN.includes(value) ? 'status-warn' : '')}>{value.replaceAll('_', ' ')}</span>;
}

export function StatCard({ label, value, hint, tone }: { label: string; value: React.ReactNode; hint?: string; tone?: 'warm' | 'alert' }) {
  return <div className={'stat-card ' + (tone || '')}><span>{label}</span><strong>{value}</strong>{hint && <small>{hint}</small>}</div>;
}

export function Avatar({ name, src, size = 36 }: { name: string; src?: string | null; size?: number }) {
  return src ? <img className="avatar-img" src={src} alt="" style={{ width: size, height: size }} />
    : <div className="mini-avatar" style={{ width: size, height: size }}>{name.slice(0, 1).toUpperCase()}</div>;
}

/* Confirm dialog and toasts, available through hooks so pages stay short. */
type Ctx = { toast: (text: string, kind?: 'success' | 'error') => void; confirm: (message: string) => Promise<boolean> };
const UiContext = createContext<Ctx>(null!);
export const useUi = () => useContext(UiContext);

export function UiProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<{ id: number; text: string; kind: string }[]>([]);
  const [dlg, setDlg] = useState<{ message: string; resolve: (v: boolean) => void } | null>(null);
  const toast = useCallback((text: string, kind: 'success' | 'error' = 'success') => {
    const id = Date.now() + Math.random();
    setToasts(t => [...t, { id, text, kind }]);
    setTimeout(() => setToasts(t => t.filter(x => x.id !== id)), 4200);
  }, []);
  const confirm = useCallback((message: string) => new Promise<boolean>(resolve => setDlg({ message, resolve })), []);
  const close = (v: boolean) => { dlg?.resolve(v); setDlg(null); };
  return <UiContext.Provider value={{ toast, confirm }}>
    {children}
    <div className="toasts" aria-live="polite">{toasts.map(t => <div key={t.id} className={'toast ' + t.kind}>{t.text}</div>)}</div>
    {dlg && <div className="modal-back" onClick={() => close(false)}><div className="modal" role="dialog" onClick={e => e.stopPropagation()}>
      <p>{dlg.message}</p><div className="modal-actions"><button className="text-btn" onClick={() => close(false)}>Keep</button><Button onClick={() => close(true)}>Yes, continue</Button></div></div></div>}
  </UiContext.Provider>;
}

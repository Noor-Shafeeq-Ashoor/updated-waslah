import axios from 'axios';

// The Vite proxy forwards /api and /auth to Spring Boot in development.
export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '', timeout: 15000 });

api.interceptors.request.use(config => {
  const token = sessionStorage.getItem('waslah_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Expired or invalid token: clear it and return to login (the login call itself is excluded).
api.interceptors.response.use(r => r, err => {
  if (axios.isAxiosError(err) && err.response?.status === 401 && !String(err.config?.url || '').includes('/auth/login')) {
    ['waslah_token', 'waslah_user', 'waslah_role'].forEach(k => sessionStorage.removeItem(k));
    if (location.pathname !== '/login') location.assign('/login');
  }
  return Promise.reject(err);
});

export function errorText(err: unknown) {
  if (axios.isAxiosError(err)) {
    const d = err.response?.data;
    return typeof d === 'string' ? d : (d?.message || d?.error || err.message);
  }
  return err instanceof Error ? err.message : 'Something went wrong';
}

export type Role = 'USER' | 'ADMIN' | 'EMPLOYER' | 'CAFE_MANAGER';
export type Page<T> = { content: T[]; totalElements: number; totalPages: number };
export type Job = { id: number; title: string; description: string; companyName: string; location: string; jobType: string; status: string; salary?: number | null; deadline?: string; createdAt?: string; createdBy?: string; vacancies?: number; filledVacancies?: number };
export type Application = { id: number; jobId: number; jobTitle: string; applicantUsername: string; status: 'PENDING' | 'ACCEPTED' | 'REJECTED'; coverLetter?: string; appliedAt?: string };
export type Cafe = { id: number; name: string; description: string; location: string; active?: boolean; managerUsername?: string };
export type Space = { id: number; name: string; capacity: number; pricePerHour: number; cafeId?: number; available?: boolean };
export type Booking = { id: number; studySpaceId: number; studySpaceCapacity: number; startTime: string; endTime: string; status: string; username?: string };
export type Session = { id: number; title: string; description: string; capacity: number; status: string; bookingId: number; studySpaceId: number; organizerUsername: string; startTime: string; endTime: string; participantCount: number; waitlistCount: number };
export type SessionEvent = { id: number; eventType: string; username?: string; details?: string; occurredAt: string };
export type AdminUser = { id: number; username: string; email: string; role: Role; status: 'ACTIVE' | 'INACTIVE'; emailVerified: boolean; firstName?: string; lastName?: string };
export type AdminStats = { totalUsers: number; activeUsers: number; inactiveUsers: number; usersByRole: Record<string, number>; pendingEmployerRequests: number; pendingCafeRequests: number; openJobs: number; pendingBookings: number; confirmedBookings: number; activeStudySessions: number };
export type EmployerRequest = { id: number; status: string; companyName?: string; companyDescription?: string; jobTypes?: string; commercialRegistration?: string; username?: string };
export type CafeRequest = { id: number; cafeName: string; description: string; location: string; status: string; requestedByUsername: string; createdAt?: string };
export type AuditLog = { id: number; actor: string; action: string; targetType: string; targetId: number; timestamp: string };
export type AppNotification = { id: number; type: string; message: string; referenceType?: string; referenceId?: number; read: boolean; createdAt: string };
export type Profile = { id?: number; firstName?: string; lastName?: string; phone?: string; bio?: string; location?: string; profileImage?: string };

export const list = <T,>(data: Page<T> | T[]): T[] => Array.isArray(data) ? data : (data?.content || []);

// Where each role lands after login.
export const homeFor = (role: string) =>
  ({ ADMIN: '/admin', EMPLOYER: '/employer', CAFE_MANAGER: '/manager' } as Record<string, string>)[role] || '/dashboard';

export const fmt = (iso?: string) => (iso ? iso.replace('T', ' ').slice(0, 16) : '—');

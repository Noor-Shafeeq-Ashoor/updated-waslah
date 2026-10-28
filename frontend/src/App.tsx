import { Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider, RequireAuth, RequireRole } from './auth';
import { UiProvider } from './ui';
import Layout from './Layout';
import { AuthPage, Landing, VerifyEmail } from './pages/public/Public';
import Profile from './pages/Profile';
import Dashboard from './pages/user/Dashboard';
import { JobDetails, Jobs } from './pages/user/Jobs';
import Applications from './pages/user/Applications';
import { CafeDetails, Cafes } from './pages/user/Cafes';
import Bookings from './pages/user/Bookings';
import { CreateSession, Sessions } from './pages/user/Sessions';
import Partner from './pages/user/Partner';
import AdminDashboard from './pages/admin/Dashboard';
import Users from './pages/admin/Users';
import Requests from './pages/admin/Requests';
import Audit from './pages/admin/Audit';
import EmployerOverview from './pages/employer/Overview';
import EmployerJobs from './pages/employer/Jobs';
import ManagerOverview from './pages/manager/Overview';
import ManagerSpaces from './pages/manager/Spaces';
import ManagerBookings from './pages/manager/Bookings';

/**
 * Each role gets its own area. RequireRole sends anyone who opens a page
 * outside their role to their own home, so admins, employers and café managers
 * never see (or can use) the member pages for applying and booking.
 */
export default function App() {
  return <AuthProvider><UiProvider><Routes>
    <Route path="/" element={<Landing />} />
    <Route path="/login" element={<AuthPage mode="login" />} />
    <Route path="/register" element={<AuthPage mode="register" />} />
    <Route path="/forgot-password" element={<AuthPage mode="forgot" />} />
    <Route path="/reset-password" element={<AuthPage mode="reset" />} />
    <Route path="/verify-email" element={<VerifyEmail />} />

    <Route element={<RequireRole allow={['USER']} />}><Route element={<Layout />}>
      <Route path="/dashboard" element={<Dashboard />} />
      <Route path="/jobs" element={<Jobs />} /><Route path="/jobs/:id" element={<JobDetails />} />
      <Route path="/applications" element={<Applications />} />
      <Route path="/cafes" element={<Cafes />} /><Route path="/cafes/:id" element={<CafeDetails />} />
      <Route path="/bookings" element={<Bookings />} />
      <Route path="/sessions" element={<Sessions />} /><Route path="/sessions/create" element={<CreateSession />} />
      <Route path="/partner" element={<Partner />} />
    </Route></Route>

    <Route element={<RequireRole allow={['ADMIN']} />}><Route element={<Layout />}>
      <Route path="/admin" element={<AdminDashboard />} /><Route path="/admin/users" element={<Users />} />
      <Route path="/admin/requests" element={<Requests />} /><Route path="/admin/audit" element={<Audit />} />
    </Route></Route>

    <Route element={<RequireRole allow={['EMPLOYER']} />}><Route element={<Layout />}>
      <Route path="/employer" element={<EmployerOverview />} /><Route path="/employer/jobs" element={<EmployerJobs />} />
    </Route></Route>

    <Route element={<RequireRole allow={['CAFE_MANAGER']} />}><Route element={<Layout />}>
      <Route path="/manager" element={<ManagerOverview />} /><Route path="/manager/spaces" element={<ManagerSpaces />} />
      <Route path="/manager/bookings" element={<ManagerBookings />} />
    </Route></Route>

    <Route element={<RequireAuth />}><Route element={<Layout />}><Route path="/profile" element={<Profile />} /></Route></Route>
    <Route path="*" element={<Navigate to="/" />} />
  </Routes></UiProvider></AuthProvider>;
}

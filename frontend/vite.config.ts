import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev server routing: each path goes to its service (same table as frontend/nginx.conf).
const identity = 'http://localhost:8081';
const career = 'http://localhost:8082';
const workspace = 'http://localhost:8083';
const notification = 'http://localhost:8084';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/auth': identity,
      '/api/users': identity,
      '/api/profile': identity,
      '/api/admin': identity,
      '/api/employer-requests': identity,
      '/api/jobs': career,
      '/api/applications': career,
      '/api/cafes': workspace,
      '/api/study-spaces': workspace,
      '/api/bookings': workspace,
      '/api/study-sessions': workspace,
      '/api/manager': workspace,
      '/api/cafe-requests': workspace,
      '/api/notifications': notification,
    },
  },
});

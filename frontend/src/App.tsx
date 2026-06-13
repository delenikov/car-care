import type { ReactElement } from 'react';
import { Navigate, RouterProvider, createBrowserRouter, useLocation } from 'react-router-dom';
import { CssBaseline, ThemeProvider } from '@mui/material';
import { QueryClientProvider } from '@tanstack/react-query';
import { theme } from './theme';
import { queryClient } from './queryClient';
import { AuthProvider, useAuth } from './auth/AuthContext';
import { ErrorBoundary } from './components/ErrorBoundary';
import { LoadingState } from './components/LoadingState';
import { ToastProvider } from './components/ToastProvider';
import { PageShell } from './components/PageShell';
import { LoginPage } from './pages/LoginPage';
import { DashboardPage } from './pages/DashboardPage';
import { ChangePasswordPage } from './pages/ChangePasswordPage';
import { ResourcePage } from './pages/ResourcePages';
import { AppointmentsPage } from './pages/AppointmentsPage';
import { ServicesPage } from './pages/ServicesPage';
import { OffersPage } from './pages/OffersPage';
import { DocumentsPage } from './pages/DocumentsPage';
import { AdminPage } from './pages/AdminPage';
import { NotFoundPage } from './pages/NotFoundPage';
import type { Role } from './types';

function RequireAuth({ children }: { children: ReactElement }) {
  const auth = useAuth();
  const location = useLocation();

  if (!auth.isReady) {
    return <LoadingState />;
  }

  if (!auth.isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  return children;
}

function RequireRole({ role, children }: { role: Role; children: ReactElement }) {
  const { user } = useAuth();

  if (!user?.roles?.includes(role)) {
    return <Navigate to="/" replace />;
  }

  return children;
}

const resourceRoutes = (kind: 'customers' | 'vehicles') => [
  { index: true, element: <ResourcePage kind={kind} mode="list" /> },
  { path: 'new', element: <ResourcePage kind={kind} mode="create" /> },
  { path: ':id', element: <ResourcePage kind={kind} mode="detail" /> },
  { path: ':id/edit', element: <ResourcePage kind={kind} mode="edit" /> }
];

const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    path: '/',
    element: (
      <RequireAuth>
        <PageShell />
      </RequireAuth>
    ),
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'change-password', element: <ChangePasswordPage /> },
      { path: 'customers', children: resourceRoutes('customers') },
      { path: 'vehicles', children: resourceRoutes('vehicles') },
      { path: 'appointments', element: <AppointmentsPage /> },
      { path: 'services', element: <ServicesPage /> },
      { path: 'services/new', element: <ServicesPage mode="create" /> },
      { path: 'offers', element: <OffersPage /> },
      { path: 'offers/new', element: <OffersPage mode="create" /> },
      { path: 'offers/:id', element: <OffersPage mode="detail" /> },
      { path: 'documents', element: <DocumentsPage /> },
      {
        path: 'admin',
        element: (
          <RequireRole role="ROLE_ADMIN">
            <AdminPage />
          </RequireRole>
        )
      },
      { path: '*', element: <NotFoundPage /> }
    ]
  }
]);

export default function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <ErrorBoundary>
        <QueryClientProvider client={queryClient}>
          <ToastProvider>
            <AuthProvider>
              <RouterProvider router={router} />
            </AuthProvider>
          </ToastProvider>
        </QueryClientProvider>
      </ErrorBoundary>
    </ThemeProvider>
  );
}

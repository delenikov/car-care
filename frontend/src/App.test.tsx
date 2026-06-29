import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { CssBaseline, ThemeProvider } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import './i18n';
import { theme } from './theme';
import { authApi } from './api/modules';
import { AuthProvider } from './auth/AuthContext';
import { ToastProvider } from './components/ToastProvider';
import { ChangePasswordPage } from './pages/ChangePasswordPage';
import { LoginPage } from './pages/LoginPage';
import { PageShell } from './components/PageShell';
import { parseSkopjeDisplayDate, parseSkopjeDisplayDateTime, skopjeDate, skopjeDisplayDate, skopjeDisplayDateTime, skopjeOffsetDateTime, skopjeTime } from './utils/dateTime';

function renderWithProviders(ui: React.ReactElement) {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  return render(
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <AuthProvider>
            <MemoryRouter>{ui}</MemoryRouter>
          </AuthProvider>
        </ToastProvider>
      </QueryClientProvider>
    </ThemeProvider>
  );
}

describe('ASMS frontend baseline', () => {
  beforeEach(() => {
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('renders the login screen', () => {
    const { container } = renderWithProviders(<LoginPage />);

    expect(container.querySelector('input[name="email"]')).toBeInTheDocument();
    expect(container.querySelector('input[name="password"]')).toBeInTheDocument();
  });

  it('shows the login credential message returned by the API', async () => {
    const user = userEvent.setup();
    vi.spyOn(authApi, 'login').mockRejectedValueOnce({ response: { data: { message: 'Лозинката е погрешна' } } });
    const { container } = renderWithProviders(<LoginPage />);

    await user.type(container.querySelector('input[name="email"]')!, 'admin@carcare.test');
    await user.type(container.querySelector('input[name="password"]')!, 'wrong-password');
    await user.click(screen.getByRole('button'));

    expect(await screen.findByRole('alert')).toHaveTextContent('Лозинката е погрешна');
  });

  it('shows the change password message returned by the API', async () => {
    const user = userEvent.setup();
    vi.spyOn(authApi, 'changePassword').mockRejectedValueOnce({ response: { data: { message: 'Тековната лозинка е погрешна' } } });
    const { container } = renderWithProviders(<ChangePasswordPage />);

    await user.type(container.querySelector('input[name="currentPassword"]')!, 'wrong-password');
    await user.type(container.querySelector('input[name="newPassword"]')!, 'password456');
    await user.click(screen.getByRole('button'));

    expect(await screen.findByRole('alert')).toHaveTextContent('Тековната лозинка е погрешна');
  });

  it('shows the admin navigation item only for admin users', async () => {
    sessionStorage.setItem('carcare.accessToken', 'access-token');
    sessionStorage.setItem('carcare.user', JSON.stringify({
      id: '2',
      email: 'tech@carcare.test',
      fullName: 'Technician',
      enabled: true,
      failedLoginAttempts: 0,
      roles: ['ROLE_EMPLOYEE']
    }));
    const employeeView = renderWithProviders(<PageShell />);

    await waitFor(() => expect(employeeView.container.querySelector('a[href="/customers"]')).toBeInTheDocument());
    expect(employeeView.container.querySelector('a[href="/admin"]')).not.toBeInTheDocument();
    employeeView.unmount();

    sessionStorage.setItem('carcare.user', JSON.stringify({
      id: '1',
      email: 'admin@carcare.test',
      fullName: 'Admin User',
      enabled: true,
      failedLoginAttempts: 0,
      roles: ['ROLE_ADMIN']
    }));
    const adminView = renderWithProviders(<PageShell />);

    await waitFor(() => expect(adminView.container.querySelector('a[href="/admin"]')).toBeInTheDocument());
  });

  it('formats Skopje date and time with minute precision and offset', () => {
    const date = '2026-06-13T12:35:22Z';

    expect(skopjeDate(date)).toBe('2026-06-13');
    expect(skopjeTime(date)).toBe('14:35');
    expect(skopjeDisplayDate(date)).toBe('13.06.2026');
    expect(skopjeDisplayDateTime(date)).toBe('13.06.2026 14:35');
    expect(parseSkopjeDisplayDate('13.06.2026')).toBe('2026-06-13');
    expect(parseSkopjeDisplayDateTime('13.06.2026 14:35')).toBe('2026-06-13T14:35');
    expect(skopjeOffsetDateTime('2026-06-13T14:35:22')).toBe('2026-06-13T14:35:22+02:00');
  });
});

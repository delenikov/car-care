import { render, screen } from '@testing-library/react';
import { CssBaseline, ThemeProvider } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import './i18n';
import { theme } from './theme';
import { ToastProvider } from './components/ToastProvider';
import { AuthProvider } from './auth/AuthContext';
import { LoginPage } from './pages/LoginPage';

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
  it('renders the Macedonian login screen', () => {
    renderWithProviders(<LoginPage />);
    expect(screen.getAllByText('Најава')[0]).toBeInTheDocument();
    expect(screen.getByLabelText('Е-пошта')).toBeInTheDocument();
  });
});

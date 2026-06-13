import { useMemo, useState } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { zodResolver } from '@hookform/resolvers/zod';
import { Box, Button, Paper, Stack, Typography } from '@mui/material';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { useAuth } from '../auth/AuthContext';
import { ApiErrorAlert, apiErrorMessage } from '../components/ApiErrorAlert';
import { FormTextField } from '../components/FormTextField';

const schema = z.object({
  email: z.string().email(),
  password: z.string().min(1)
});

type LoginForm = z.infer<typeof schema>;

export function LoginPage() {
  const { t } = useTranslation();
  const auth = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [errorMessage, setErrorMessage] = useState('');
  const from = useMemo(() => (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ?? '/', [location.state]);
  const { control, handleSubmit, formState } = useForm<LoginForm>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', password: '' }
  });

  if (auth.isAuthenticated) {
    return <Navigate to="/" replace />;
  }

  const onSubmit = handleSubmit(async (values) => {
    setErrorMessage('');
    try {
      await auth.login(values);
      navigate(from, { replace: true });
    } catch (error) {
      setErrorMessage(apiErrorMessage(error, 'Login failed'));
    }
  });

  return (
    <Box sx={{ minHeight: '100vh', display: 'grid', gridTemplateColumns: { xs: '1fr', md: '1.1fr 0.9fr' } }}>
      <Box sx={{ p: { xs: 3, md: 7 }, display: 'flex', alignItems: 'center' }}>
        <Stack spacing={4} sx={{ maxWidth: 680 }}>
          <Typography variant="overline" color="secondary.dark">
            Automotive service management
          </Typography>
          <Typography variant="h1" sx={{ fontSize: { xs: '3rem', md: '5.5rem' }, lineHeight: 0.9 }}>
            Сервисот работи во ритам.
          </Typography>
          <Typography variant="h6" color="text.secondary" sx={{ maxWidth: 560 }}>
            Еден центар за клиенти, возила, термини, понуди и документи - дизајниран за брз прием и прецизна работилница.
          </Typography>
        </Stack>
      </Box>
      <Box sx={{ display: 'grid', placeItems: 'center', p: { xs: 2, md: 5 } }}>
        <Paper component="form" onSubmit={onSubmit} sx={{ width: '100%', maxWidth: 460, p: { xs: 3, md: 5 }, borderRadius: 5 }}>
          <Stack spacing={3}>
            <Box>
              <Typography variant="h3">{t('login')}</Typography>
              <Typography color="text.secondary">{t('appName')}</Typography>
            </Box>
            <ApiErrorAlert message={errorMessage} />
            <FormTextField control={control} name="email" label={t('email')} autoComplete="email" />
            <FormTextField control={control} name="password" label={t('password')} type="password" autoComplete="current-password" />
            <Button type="submit" variant="contained" size="large" disabled={formState.isSubmitting}>
              {t('login')}
            </Button>
          </Stack>
        </Paper>
      </Box>
    </Box>
  );
}

import { useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { Button, Paper, Stack, Typography } from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { authApi } from '../api/modules';
import { ApiErrorAlert, apiErrorMessage } from '../components/ApiErrorAlert';
import { FormTextField } from '../components/FormTextField';
import { useToast } from '../components/ToastProvider';

const schema = z.object({
  currentPassword: z.string().min(1),
  newPassword: z.string().min(8)
});

type ChangePasswordForm = z.infer<typeof schema>;

export function ChangePasswordPage() {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const [errorMessage, setErrorMessage] = useState('');
  const { control, handleSubmit, reset, formState } = useForm<ChangePasswordForm>({
    resolver: zodResolver(schema),
    defaultValues: { currentPassword: '', newPassword: '' }
  });
  const mutation = useMutation({ mutationFn: authApi.changePassword });

  const onSubmit = handleSubmit(async (values) => {
    setErrorMessage('');
    try {
      await mutation.mutateAsync(values);
      reset();
      showToast(t('updated'));
    } catch (error) {
      setErrorMessage(apiErrorMessage(error, 'Password change failed'));
    }
  });

  return (
    <Stack spacing={3} sx={{ maxWidth: 680 }}>
      <Typography variant="h2">{t('changePassword')}</Typography>
      <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 } }}>
        <Stack spacing={3}>
          <ApiErrorAlert message={errorMessage} />
          <FormTextField control={control} name="currentPassword" label="Тековна лозинка" type="password" />
          <FormTextField control={control} name="newPassword" label="Нова лозинка" type="password" />
          <Button type="submit" variant="contained" disabled={formState.isSubmitting}>
            {t('save')}
          </Button>
        </Stack>
      </Paper>
    </Stack>
  );
}

import { useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { Stack } from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { authApi } from '../api/modules';
import { ApiErrorAlert, apiErrorMessage } from './ApiErrorAlert';
import { FormDialog } from './FormDialog';
import { FormTextField } from './FormTextField';
import { useToast } from './ToastProvider';
import { applyApiFieldErrors } from '../utils/apiFormErrors';

const schema = z.object({
  currentPassword: z.string().min(1),
  newPassword: z.string().min(8)
});

type ChangePasswordForm = z.infer<typeof schema>;

export function ChangePasswordDialog({ onClose }: { onClose: () => void }) {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const [errorMessage, setErrorMessage] = useState('');
  const { control, handleSubmit, reset, setError, formState } = useForm<ChangePasswordForm>({
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
      onClose();
    } catch (error) {
      applyApiFieldErrors(error, setError);
      setErrorMessage(apiErrorMessage(error, t('passwordChangeFailed')));
    }
  });

  return (
    <FormDialog title={t('changePassword')} onClose={onClose} onSubmit={onSubmit} isSubmitting={formState.isSubmitting}>
      <Stack spacing={3}>
        <ApiErrorAlert message={errorMessage} />
        <FormTextField control={control} name="currentPassword" label={t('currentPassword')} type="password" autoComplete="current-password" />
        <FormTextField control={control} name="newPassword" label={t('newPassword')} type="password" autoComplete="new-password" />
      </Stack>
    </FormDialog>
  );
}

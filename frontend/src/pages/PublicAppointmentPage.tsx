import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Box, Button, Chip, Paper, Stack, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { appointmentsApi } from '../api/modules';
import { ApiErrorAlert, apiErrorMessage } from '../components/ApiErrorAlert';
import { DateInput } from '../components/DateTimeInputs';
import { FormTextField } from '../components/FormTextField';
import { useToast } from '../components/ToastProvider';
import { applyApiFieldErrors } from '../utils/apiFormErrors';
import { skopjeDate, skopjeDateTimeInput, skopjeOffsetDateTime, skopjeTime } from '../utils/dateTime';

const schema = z.object({
  fullName: z.string().min(1),
  email: z.string().email(),
  phone: z.string().optional(),
  plateNumber: z.string().min(1),
  vin: z.string().optional(),
  make: z.string().min(1),
  model: z.string().min(1),
  modelYear: z.coerce.number().optional(),
  engine: z.string().optional(),
  fuelType: z.string().optional(),
  startsAt: z.string().min(1),
  endsAt: z.string().min(1),
  serviceType: z.string().min(1),
  notes: z.string().optional()
});

type PublicAppointmentForm = z.output<typeof schema>;

export function PublicAppointmentPage() {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const queryClient = useQueryClient();
  const [slotDate, setSlotDate] = useState(skopjeDate(new Date()));
  const [errorMessage, setErrorMessage] = useState('');
  const availableQuery = useQuery({ queryKey: ['appointments', 'available', slotDate, 'public'], queryFn: () => appointmentsApi.available(slotDate) });
  const createMutation = useMutation({ mutationFn: appointmentsApi.publicCreate });
  const { control, handleSubmit, reset, setError, setValue, formState } = useForm<PublicAppointmentForm>({
    resolver: zodResolver(schema) as never,
    defaultValues: {
      fullName: '',
      email: '',
      phone: '',
      plateNumber: '',
      vin: '',
      make: '',
      model: '',
      modelYear: new Date().getFullYear(),
      engine: '',
      fuelType: '',
      startsAt: '',
      endsAt: '',
      serviceType: '',
      notes: ''
    }
  });
  const selectedStart = useWatch({ control, name: 'startsAt' });

  const onSubmit = handleSubmit(async (values) => {
    setErrorMessage('');
    try {
      await createMutation.mutateAsync({
        ...values,
        startsAt: skopjeOffsetDateTime(values.startsAt),
        endsAt: skopjeOffsetDateTime(values.endsAt)
      });
      await queryClient.invalidateQueries({ queryKey: ['appointments', 'available'] });
      reset({ ...values, startsAt: '', endsAt: '' });
      showToast(t('saved'));
    } catch (error) {
      applyApiFieldErrors(error, setError);
      setErrorMessage(apiErrorMessage(error, t('appointmentSaveFailed')));
    }
  });

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', p: { xs: 2, md: 4 } }}>
      <Stack spacing={3} sx={{ maxWidth: 920, mx: 'auto' }}>
        <Box>
          <Typography variant="h2">{t('bookAppointment')}</Typography>
        </Box>
        <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 } }}>
          <Stack spacing={3}>
            <ApiErrorAlert message={errorMessage} />
            <DateInput
              name="slotDate"
              label={t('date')}
              value={slotDate}
              error={false}
              onBlur={() => undefined}
              onChange={(value) => setSlotDate(value)}
              helperText="DD.MM.YYYY"
            />
            {availableQuery.isError ? (
              <ApiErrorAlert message={apiErrorMessage(availableQuery.error, t('loadFailed'))} />
            ) : availableQuery.data?.length === 0 && !availableQuery.isLoading ? (
              <Alert severity="warning" variant="filled">{t('noAvailableSlots')}</Alert>
            ) : (
              <Stack direction="row" flexWrap="wrap" gap={1}>
                {(availableQuery.data ?? []).map((slot) => (
                  <Chip
                    key={slot.startsAt}
                    color={selectedStart === skopjeDateTimeInput(slot.startsAt) ? 'primary' : 'default'}
                    label={skopjeTime(slot.startsAt)}
                    onClick={() => {
                      setValue('startsAt', skopjeDateTimeInput(slot.startsAt), { shouldDirty: true, shouldValidate: true });
                      setValue('endsAt', skopjeDateTimeInput(slot.endsAt), { shouldDirty: true, shouldValidate: true });
                    }}
                  />
                ))}
              </Stack>
            )}
            <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2 }}>
              <FormTextField control={control} name="fullName" label={t('fullName')} />
              <FormTextField control={control} name="email" label={t('email')} type="email" />
              <FormTextField control={control} name="phone" label={t('phone')} />
              <FormTextField control={control} name="serviceType" label={t('serviceType')} />
              <FormTextField control={control} name="plateNumber" label={t('licensePlate')} />
              <FormTextField control={control} name="vin" label={t('vin')} />
              <FormTextField control={control} name="make" label={t('brand')} />
              <FormTextField control={control} name="model" label={t('model')} />
              <FormTextField control={control} name="modelYear" label={t('year')} type="number" />
              <FormTextField control={control} name="engine" label={t('engine')} />
              <FormTextField control={control} name="fuelType" label={t('fuelType')} />
              <FormTextField control={control} name="notes" label={t('notes')} multiline minRows={3} sx={{ gridColumn: { md: '1 / -1' } }} />
            </Box>
            <Controller control={control} name="startsAt" render={() => <input type="hidden" />} />
            <Controller control={control} name="endsAt" render={() => <input type="hidden" />} />
            <Button type="submit" variant="contained" disabled={formState.isSubmitting || !selectedStart}>
              {t('save')}
            </Button>
          </Stack>
        </Paper>
      </Stack>
    </Box>
  );
}

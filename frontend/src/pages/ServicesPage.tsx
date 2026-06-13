import { zodResolver } from '@hookform/resolvers/zod';
import { Box, Button, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Controller, useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { serviceRecordsApi } from '../api/modules';
import { FormTextField } from '../components/FormTextField';
import { EmptyState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';
import { parseSkopjeDisplayDate, skopjeDate, skopjeDisplayDate, skopjeTime } from '../utils/dateTime';

const schema = z.object({
  customerId: z.string().min(1),
  vehicleId: z.string().min(1),
  performedAt: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
  serviceTime: z.string().regex(/^\d{2}:\d{2}:\d{2}$/),
  mileage: z.coerce.number().min(0),
  summary: z.string().min(1),
  partsCost: z.coerce.number().min(0),
  laborCost: z.coerce.number().min(0),
  replacedParts: z.string().optional(),
  notes: z.string().optional()
});

type ServiceForm = z.output<typeof schema>;

export function ServicesPage({ mode = 'list' }: { mode?: 'list' | 'create' }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const listQuery = useQuery({ queryKey: ['service-records'], queryFn: serviceRecordsApi.list, enabled: mode === 'list' });
  const createMutation = useMutation({ mutationFn: serviceRecordsApi.create });
  const { control, handleSubmit, formState } = useForm<ServiceForm>({
    resolver: zodResolver(schema) as never,
    defaultValues: {
      customerId: '',
      vehicleId: '',
      performedAt: skopjeDate(new Date()),
      serviceTime: skopjeTime(new Date()),
      mileage: 0,
      summary: 'Minor Service',
      partsCost: 0,
      laborCost: 0,
      replacedParts: '',
      notes: ''
    }
  });

  if (mode === 'create') {
    const onSubmit = handleSubmit(async (values) => {
      const { serviceTime: _serviceTime, ...serviceValues } = values;
      void _serviceTime;
      await createMutation.mutateAsync({ ...serviceValues, cost: values.partsCost + values.laborCost });
      await queryClient.invalidateQueries({ queryKey: ['service-records'] });
      showToast(t('saved'));
      navigate('/services');
    });

    return (
      <Stack spacing={3}>
        <Typography variant="h2">{t('newService')}</Typography>
        <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 } }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 3 }}>
            <FormTextField control={control} name="customerId" label="Customer ID" />
            <FormTextField control={control} name="vehicleId" label="Vehicle ID" />
            <Controller
              control={control}
              name="performedAt"
              render={({ field, fieldState }) => (
                <TextField
                  name={field.name}
                  label="Date"
                  value={/^\d{4}-\d{2}-\d{2}$/.test(field.value) ? skopjeDisplayDate(field.value) : field.value}
                  placeholder="13.06.2026."
                  helperText={fieldState.error?.message ?? 'DD.MM.YYYY.'}
                  error={Boolean(fieldState.error)}
                  onBlur={field.onBlur}
                  onChange={(event) => field.onChange(parseSkopjeDisplayDate(event.target.value) ?? event.target.value)}
                  inputRef={field.ref}
                />
              )}
            />
            <FormTextField control={control} name="serviceTime" label="Time" placeholder="14:35:22" helperText="HH:mm:ss" />
            <FormTextField control={control} name="mileage" label="Mileage" type="number" />
            <FormTextField control={control} name="summary" label="Service type" />
            <FormTextField control={control} name="replacedParts" label="Replaced parts" />
            <FormTextField control={control} name="partsCost" label="Parts cost" type="number" />
            <FormTextField control={control} name="laborCost" label="Labor cost" type="number" />
            <FormTextField control={control} name="notes" label="Notes" multiline minRows={4} sx={{ gridColumn: { md: '1 / -1' } }} />
          </Box>
          <Button sx={{ mt: 3 }} type="submit" variant="contained" disabled={formState.isSubmitting}>
            {t('save')}
          </Button>
        </Paper>
      </Stack>
    );
  }

  if (listQuery.isLoading) return <LoadingState />;
  const records = listQuery.data ?? [];

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }}>
        <Typography variant="h2">{t('services')}</Typography>
        <Button component={RouterLink} to="/services/new" variant="contained">
          {t('newService')}
        </Button>
      </Stack>
      {records.length ? (
        <Paper sx={{ overflow: 'auto' }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Vehicle</TableCell>
                <TableCell>Date</TableCell>
                <TableCell>Service</TableCell>
                <TableCell align="right">Parts</TableCell>
                <TableCell align="right">Labor</TableCell>
                <TableCell align="right">Total</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {records.map((record) => (
                <TableRow key={record.id} hover>
                  <TableCell>{record.vehicleId}</TableCell>
                  <TableCell>{record.performedAt}</TableCell>
                  <TableCell>{record.summary}</TableCell>
                  <TableCell align="right">{record.partsCost.toLocaleString('mk-MK')} den.</TableCell>
                  <TableCell align="right">{record.laborCost.toLocaleString('mk-MK')} den.</TableCell>
                  <TableCell align="right">{record.cost.toLocaleString('mk-MK')} den.</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      ) : (
        <EmptyState />
      )}
    </Stack>
  );
}

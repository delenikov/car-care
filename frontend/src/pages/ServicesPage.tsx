import { zodResolver } from '@hookform/resolvers/zod';
import { Box, Button, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { z } from 'zod';
import { serviceRecordsApi } from '../api/modules';
import { FormTextField } from '../components/FormTextField';
import { EmptyState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';

const schema = z.object({
  vehicleId: z.string().min(1),
  performedAt: z.string().min(1),
  mileage: z.coerce.number().min(0),
  summary: z.string().min(1),
  cost: z.coerce.number().min(0)
});

type ServiceForm = z.infer<typeof schema>;

export function ServicesPage({ mode = 'list' }: { mode?: 'list' | 'create' }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const listQuery = useQuery({ queryKey: ['service-records'], queryFn: serviceRecordsApi.list, enabled: mode === 'list' });
  const createMutation = useMutation({ mutationFn: serviceRecordsApi.create });
  const { control, handleSubmit, formState } = useForm<ServiceForm>({
    resolver: zodResolver(schema),
    defaultValues: { vehicleId: '', performedAt: new Date().toISOString().slice(0, 10), mileage: 0, summary: '', cost: 0 }
  });

  if (mode === 'create') {
    const onSubmit = handleSubmit(async (values) => {
      await createMutation.mutateAsync(values);
      await queryClient.invalidateQueries({ queryKey: ['service-records'] });
      showToast(t('saved'));
      navigate('/services');
    });

    return (
      <Stack spacing={3}>
        <Typography variant="h2">{t('newService')}</Typography>
        <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 } }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 3 }}>
            <FormTextField control={control} name="vehicleId" label="ID на возило" />
            <FormTextField control={control} name="performedAt" label="Датум" type="date" InputLabelProps={{ shrink: true }} />
            <FormTextField control={control} name="mileage" label="Километража" type="number" />
            <FormTextField control={control} name="cost" label="Цена" type="number" />
            <FormTextField control={control} name="summary" label="Извршена работа" multiline minRows={4} sx={{ gridColumn: { md: '1 / -1' } }} />
          </Box>
          <Button sx={{ mt: 3 }} type="submit" variant="contained" disabled={formState.isSubmitting}>{t('save')}</Button>
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
        <Button component={RouterLink} to="/services/new" variant="contained">{t('newService')}</Button>
      </Stack>
      {records.length ? (
        <Paper sx={{ overflow: 'auto' }}>
          <Table>
            <TableHead><TableRow><TableCell>Возило</TableCell><TableCell>Датум</TableCell><TableCell>Опис</TableCell><TableCell align="right">Цена</TableCell></TableRow></TableHead>
            <TableBody>
              {records.map((record) => (
                <TableRow key={record.id} hover>
                  <TableCell>{record.vehicleId}</TableCell>
                  <TableCell>{new Date(record.performedAt).toLocaleDateString('mk-MK')}</TableCell>
                  <TableCell>{record.summary}</TableCell>
                  <TableCell align="right">{record.cost.toLocaleString('mk-MK')} ден.</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      ) : <EmptyState />}
    </Stack>
  );
}

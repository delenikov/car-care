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
  customerId: z.string().min(1),
  vehicleId: z.string().min(1),
  performedAt: z.string().min(1),
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
      performedAt: new Date().toISOString().slice(0, 10),
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
      await createMutation.mutateAsync({ ...values, cost: values.partsCost + values.laborCost });
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
            <FormTextField control={control} name="performedAt" label="Date" type="date" InputLabelProps={{ shrink: true }} />
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
                  <TableCell>{new Date(record.performedAt).toLocaleDateString('mk-MK')}</TableCell>
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

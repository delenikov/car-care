import { useEffect } from 'react';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { zodResolver } from '@hookform/resolvers/zod';
import { Box, Button, Chip, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { customersApi, vehiclesApi } from '../api/modules';
import { FormTextField } from '../components/FormTextField';
import { EmptyState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';
import type { Customer, Vehicle } from '../types';

type Kind = 'customers' | 'vehicles';
type Mode = 'list' | 'create' | 'detail' | 'edit';

const customerSchema = z.object({
  name: z.string().min(1, 'Задолжително име'),
  phone: z.string().min(1, 'Задолжителен телефон'),
  email: z.string().email('Невалидна е-пошта').optional().or(z.literal('')),
  loyaltyPoints: z.coerce.number().min(0),
  notes: z.string().optional()
});

const vehicleSchema = z.object({
  customerId: z.string().min(1, 'Задолжителен клиент'),
  plate: z.string().min(1, 'Задолжителна регистрација'),
  make: z.string().min(1, 'Задолжителна марка'),
  model: z.string().min(1, 'Задолжителен модел'),
  year: z.coerce.number().min(1950),
  vin: z.string().optional()
});

type CustomerForm = z.infer<typeof customerSchema>;
type VehicleForm = z.infer<typeof vehicleSchema>;

export function ResourcePage({ kind, mode }: { kind: Kind; mode: Mode }) {
  if (kind === 'customers') {
    return <CustomerPage mode={mode} />;
  }
  return <VehiclePage mode={mode} />;
}

function CustomerPage({ mode }: { mode: Mode }) {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const listQuery = useQuery({ queryKey: ['customers'], queryFn: customersApi.list, enabled: mode === 'list' });
  const detailQuery = useQuery({ queryKey: ['customers', id], queryFn: () => customersApi.get(id!), enabled: Boolean(id) && mode !== 'create' && mode !== 'list' });
  const { control, handleSubmit, reset, formState } = useForm<CustomerForm>({
    resolver: zodResolver(customerSchema),
    defaultValues: { name: '', phone: '', email: '', loyaltyPoints: 0, notes: '' }
  });
  const createMutation = useMutation({ mutationFn: customersApi.create });
  const updateMutation = useMutation({ mutationFn: (values: CustomerForm) => customersApi.update(id!, values) });

  useEffect(() => {
    if (detailQuery.data) {
      reset(detailQuery.data);
    }
  }, [detailQuery.data, reset]);

  if (mode === 'list') {
    if (listQuery.isLoading) return <LoadingState />;
    return (
      <ResourceFrame title={t('customers')} actionLabel={t('create')} actionTo="/customers/new">
        <CustomerTable customers={listQuery.data ?? []} />
      </ResourceFrame>
    );
  }

  if (mode === 'detail') {
    if (detailQuery.isLoading) return <LoadingState />;
    if (!detailQuery.data) return <EmptyState />;
    return (
      <ResourceFrame title={detailQuery.data.name} actionLabel={t('edit')} actionTo={`/customers/${id}/edit`}>
        <DetailCard rows={[['Телефон', detailQuery.data.phone], ['Е-пошта', detailQuery.data.email ?? '-'], ['Поени', String(detailQuery.data.loyaltyPoints)], ['Белешки', detailQuery.data.notes ?? '-']]} />
      </ResourceFrame>
    );
  }

  const onSubmit = handleSubmit(async (values) => {
    const saved = mode === 'create' ? await createMutation.mutateAsync(values) : await updateMutation.mutateAsync(values);
    await queryClient.invalidateQueries({ queryKey: ['customers'] });
    showToast(t(mode === 'create' ? 'saved' : 'updated'));
    navigate(`/customers/${saved.id}`);
  });

  return (
    <ResourceFrame title={mode === 'create' ? `${t('create')} ${t('customers')}` : `${t('edit')} ${detailQuery.data?.name ?? ''}`}>
      <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 } }}>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 3 }}>
          <FormTextField control={control} name="name" label="Име и презиме" />
          <FormTextField control={control} name="phone" label="Телефон" />
          <FormTextField control={control} name="email" label="Е-пошта" />
          <FormTextField control={control} name="loyaltyPoints" label="Поени" type="number" />
          <FormTextField control={control} name="notes" label="Белешки" multiline minRows={3} sx={{ gridColumn: { md: '1 / -1' } }} />
        </Box>
        <Button sx={{ mt: 3 }} type="submit" variant="contained" disabled={formState.isSubmitting}>
          {t('save')}
        </Button>
      </Paper>
    </ResourceFrame>
  );
}

function VehiclePage({ mode }: { mode: Mode }) {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const listQuery = useQuery({ queryKey: ['vehicles'], queryFn: vehiclesApi.list, enabled: mode === 'list' });
  const detailQuery = useQuery({ queryKey: ['vehicles', id], queryFn: () => vehiclesApi.get(id!), enabled: Boolean(id) && mode !== 'create' && mode !== 'list' });
  const { control, handleSubmit, reset, formState } = useForm<VehicleForm>({
    resolver: zodResolver(vehicleSchema),
    defaultValues: { customerId: '', plate: '', make: '', model: '', year: new Date().getFullYear(), vin: '' }
  });
  const createMutation = useMutation({ mutationFn: vehiclesApi.create });
  const updateMutation = useMutation({ mutationFn: (values: VehicleForm) => vehiclesApi.update(id!, values) });

  useEffect(() => {
    if (detailQuery.data) {
      reset(detailQuery.data);
    }
  }, [detailQuery.data, reset]);

  if (mode === 'list') {
    if (listQuery.isLoading) return <LoadingState />;
    return (
      <ResourceFrame title={t('vehicles')} actionLabel={t('create')} actionTo="/vehicles/new">
        <VehicleTable vehicles={listQuery.data ?? []} />
      </ResourceFrame>
    );
  }

  if (mode === 'detail') {
    if (detailQuery.isLoading) return <LoadingState />;
    if (!detailQuery.data) return <EmptyState />;
    return (
      <ResourceFrame title={`${detailQuery.data.make} ${detailQuery.data.model}`} actionLabel={t('edit')} actionTo={`/vehicles/${id}/edit`}>
        <DetailCard rows={[['Регистрација', detailQuery.data.plate], ['Година', String(detailQuery.data.year)], ['Клиент', detailQuery.data.customerId], ['VIN', detailQuery.data.vin ?? '-']]} />
      </ResourceFrame>
    );
  }

  const onSubmit = handleSubmit(async (values) => {
    const saved = mode === 'create' ? await createMutation.mutateAsync(values) : await updateMutation.mutateAsync(values);
    await queryClient.invalidateQueries({ queryKey: ['vehicles'] });
    showToast(t(mode === 'create' ? 'saved' : 'updated'));
    navigate(`/vehicles/${saved.id}`);
  });

  return (
    <ResourceFrame title={mode === 'create' ? `${t('create')} ${t('vehicles')}` : `${t('edit')} ${detailQuery.data?.plate ?? ''}`}>
      <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 } }}>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 3 }}>
          <FormTextField control={control} name="customerId" label="ID на клиент" />
          <FormTextField control={control} name="plate" label="Регистрација" />
          <FormTextField control={control} name="make" label="Марка" />
          <FormTextField control={control} name="model" label="Модел" />
          <FormTextField control={control} name="year" label="Година" type="number" />
          <FormTextField control={control} name="vin" label="VIN" />
        </Box>
        <Button sx={{ mt: 3 }} type="submit" variant="contained" disabled={formState.isSubmitting}>
          {t('save')}
        </Button>
      </Paper>
    </ResourceFrame>
  );
}

function ResourceFrame({ title, children, actionLabel, actionTo }: { title: string; children: React.ReactNode; actionLabel?: string; actionTo?: string }) {
  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }}>
        <Typography variant="h2">{title}</Typography>
        {actionTo && actionLabel ? (
          <Button component={RouterLink} to={actionTo} variant="contained">
            {actionLabel}
          </Button>
        ) : null}
      </Stack>
      {children}
    </Stack>
  );
}

function CustomerTable({ customers }: { customers: Customer[] }) {
  if (!customers.length) return <EmptyState />;
  return (
    <Paper sx={{ overflow: 'auto' }}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Клиент</TableCell>
            <TableCell>Телефон</TableCell>
            <TableCell>Лојалност</TableCell>
            <TableCell align="right">Акција</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {customers.map((customer) => (
            <TableRow key={customer.id} hover>
              <TableCell>{customer.name}</TableCell>
              <TableCell>{customer.phone}</TableCell>
              <TableCell><Chip label={customer.loyaltyPoints} color="secondary" size="small" /></TableCell>
              <TableCell align="right"><Button component={RouterLink} to={`/customers/${customer.id}`}>Детали</Button></TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Paper>
  );
}

function VehicleTable({ vehicles }: { vehicles: Vehicle[] }) {
  if (!vehicles.length) return <EmptyState />;
  return (
    <Paper sx={{ overflow: 'auto' }}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>Регистрација</TableCell>
            <TableCell>Возило</TableCell>
            <TableCell>Година</TableCell>
            <TableCell align="right">Акција</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {vehicles.map((vehicle) => (
            <TableRow key={vehicle.id} hover>
              <TableCell>{vehicle.plate}</TableCell>
              <TableCell>{vehicle.make} {vehicle.model}</TableCell>
              <TableCell>{vehicle.year}</TableCell>
              <TableCell align="right"><Button component={RouterLink} to={`/vehicles/${vehicle.id}`}>Детали</Button></TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Paper>
  );
}

function DetailCard({ rows }: { rows: Array<[string, string]> }) {
  return (
    <Paper sx={{ p: { xs: 3, md: 4 } }}>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2 }}>
        {rows.map(([label, value]) => (
          <Box key={label} sx={{ p: 2, border: 1, borderColor: 'divider', borderRadius: 3 }}>
            <Typography variant="body2" color="text.secondary">{label}</Typography>
            <Typography fontWeight={700}>{value}</Typography>
          </Box>
        ))}
      </Box>
    </Paper>
  );
}

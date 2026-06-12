import { useEffect, useState } from 'react';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Box,
  Button,
  Chip,
  Divider,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography
} from '@mui/material';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import SearchRoundedIcon from '@mui/icons-material/SearchRounded';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { customersApi, vehiclesApi } from '../api/modules';
import { FormTextField } from '../components/FormTextField';
import { EmptyState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';
import type { Customer, ServiceRecord, Vehicle } from '../types';

type Kind = 'customers' | 'vehicles';
type Mode = 'list' | 'create' | 'detail' | 'edit';

const customerSchema = z.object({
  name: z.string().min(1, 'Customer name is required'),
  phone: z.string().min(1, 'Phone is required'),
  email: z.string().email('Invalid email').optional().or(z.literal('')),
  loyaltyPoints: z.coerce.number().min(0),
  notes: z.string().optional()
});

const vehicleSchema = z.object({
  customerId: z.string().min(1, 'Customer is required'),
  plate: z.string().min(1, 'License plate is required'),
  make: z.string().min(1, 'Brand is required'),
  model: z.string().min(1, 'Model is required'),
  year: z.coerce.number().min(1950),
  vin: z.string().optional()
});

type CustomerForm = z.output<typeof customerSchema>;
type VehicleForm = z.output<typeof vehicleSchema>;

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
  const [filters, setFilters] = useState({ firstName: '', lastName: '' });
  const [submittedFilters, setSubmittedFilters] = useState(filters);
  const listQuery = useQuery({
    queryKey: ['customers', submittedFilters],
    queryFn: () => customersApi.list(cleanFilters(submittedFilters)),
    enabled: mode === 'list'
  });
  const detailQuery = useQuery({
    queryKey: ['customers', id],
    queryFn: () => customersApi.get(id!),
    enabled: Boolean(id) && mode !== 'create' && mode !== 'list'
  });
  const vehiclesQuery = useQuery({
    queryKey: ['customers', id, 'vehicles'],
    queryFn: () => customersApi.vehicles(id!),
    enabled: Boolean(id) && mode === 'detail'
  });
  const historyQuery = useQuery({
    queryKey: ['customers', id, 'service-history'],
    queryFn: () => customersApi.serviceHistory(id!),
    enabled: Boolean(id) && mode === 'detail'
  });
  const { control, handleSubmit, reset, formState } = useForm<CustomerForm>({
    resolver: zodResolver(customerSchema) as never,
    defaultValues: { name: '', phone: '', email: '', loyaltyPoints: 0, notes: '' }
  });
  const createMutation = useMutation({ mutationFn: customersApi.create });
  const updateMutation = useMutation({ mutationFn: (values: CustomerForm) => customersApi.update(id!, values) });
  const deleteMutation = useMutation({ mutationFn: (customerId: string) => customersApi.remove(customerId) });

  useEffect(() => {
    if (detailQuery.data) {
      reset(detailQuery.data);
    }
  }, [detailQuery.data, reset]);

  if (mode === 'list') {
    if (listQuery.isLoading) return <LoadingState />;
    return (
      <ResourceFrame title={t('customers')} actionLabel={t('create')} actionTo="/customers/new">
        <Paper sx={{ p: 2 }}>
          <Stack
            direction={{ xs: 'column', md: 'row' }}
            spacing={2}
            component="form"
            onSubmit={(event) => {
              event.preventDefault();
              setSubmittedFilters(filters);
            }}
          >
            <TextField
              name="firstName"
              label="First name"
              value={filters.firstName}
              onChange={(event) => setFilters((current) => ({ ...current, firstName: event.target.value }))}
              fullWidth
            />
            <TextField
              name="lastName"
              label="Last name"
              value={filters.lastName}
              onChange={(event) => setFilters((current) => ({ ...current, lastName: event.target.value }))}
              fullWidth
            />
            <Button type="submit" variant="outlined" startIcon={<SearchRoundedIcon />} sx={{ minWidth: 150 }}>
              Search
            </Button>
          </Stack>
        </Paper>
        <CustomerTable customers={listQuery.data ?? []} />
      </ResourceFrame>
    );
  }

  if (mode === 'detail') {
    if (detailQuery.isLoading) return <LoadingState />;
    if (!detailQuery.data) return <EmptyState />;
    return (
      <ResourceFrame title={detailQuery.data.name} actionLabel={t('edit')} actionTo={`/customers/${id}/edit`}>
        <Stack spacing={3}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="flex-end">
            <Button
              color="error"
              variant="outlined"
              startIcon={<DeleteOutlineRoundedIcon />}
              disabled={deleteMutation.isPending}
              onClick={async () => {
                await deleteMutation.mutateAsync(id!);
                await queryClient.invalidateQueries({ queryKey: ['customers'] });
                showToast(t('deleted'));
                navigate('/customers');
              }}
            >
              Delete
            </Button>
          </Stack>
          <DetailCard
            rows={[
              ['Phone', detailQuery.data.phone],
              ['Email', detailQuery.data.email ?? '-'],
              ['Loyalty points', String(detailQuery.data.loyaltyPoints)],
              ['Address', detailQuery.data.notes ?? '-']
            ]}
          />
          <RelatedVehicles vehicles={vehiclesQuery.data ?? []} loading={vehiclesQuery.isLoading} />
          <ServiceHistory records={historyQuery.data ?? []} loading={historyQuery.isLoading} />
        </Stack>
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
          <FormTextField control={control} name="name" label="Full name" />
          <FormTextField control={control} name="phone" label="Phone" />
          <FormTextField control={control} name="email" label="Email" />
          <FormTextField control={control} name="loyaltyPoints" label="Loyalty points" type="number" />
          <FormTextField control={control} name="notes" label="Address" multiline minRows={3} sx={{ gridColumn: { md: '1 / -1' } }} />
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
  const [filters, setFilters] = useState({ vin: '', plateNumber: '', owner: '' });
  const [submittedFilters, setSubmittedFilters] = useState(filters);
  const listQuery = useQuery({
    queryKey: ['vehicles', submittedFilters],
    queryFn: () => vehiclesApi.list(cleanVehicleFilters(submittedFilters)),
    enabled: mode === 'list'
  });
  const detailQuery = useQuery({
    queryKey: ['vehicles', id],
    queryFn: () => vehiclesApi.get(id!),
    enabled: Boolean(id) && mode !== 'create' && mode !== 'list'
  });
  const historyQuery = useQuery({
    queryKey: ['vehicles', id, 'service-history'],
    queryFn: () => vehiclesApi.serviceHistory(id!),
    enabled: Boolean(id) && mode === 'detail'
  });
  const { control, handleSubmit, reset, formState } = useForm<VehicleForm>({
    resolver: zodResolver(vehicleSchema) as never,
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
        <Paper sx={{ p: 2 }}>
          <Stack
            direction={{ xs: 'column', md: 'row' }}
            spacing={2}
            component="form"
            onSubmit={(event) => {
              event.preventDefault();
              setSubmittedFilters(filters);
            }}
          >
            <TextField
              name="vin"
              label="VIN"
              value={filters.vin}
              onChange={(event) => setFilters((current) => ({ ...current, vin: event.target.value }))}
              fullWidth
            />
            <TextField
              name="plateNumber"
              label="License plate"
              value={filters.plateNumber}
              onChange={(event) => setFilters((current) => ({ ...current, plateNumber: event.target.value }))}
              fullWidth
            />
            <TextField
              name="owner"
              label="Owner"
              value={filters.owner}
              onChange={(event) => setFilters((current) => ({ ...current, owner: event.target.value }))}
              fullWidth
            />
            <Button type="submit" variant="outlined" startIcon={<SearchRoundedIcon />} sx={{ minWidth: 150 }}>
              Search
            </Button>
          </Stack>
        </Paper>
        <VehicleTable vehicles={listQuery.data ?? []} />
      </ResourceFrame>
    );
  }

  if (mode === 'detail') {
    if (detailQuery.isLoading) return <LoadingState />;
    if (!detailQuery.data) return <EmptyState />;
    return (
      <ResourceFrame title={`${detailQuery.data.make} ${detailQuery.data.model}`} actionLabel={t('edit')} actionTo={`/vehicles/${id}/edit`}>
        <Stack spacing={3}>
          <DetailCard
            rows={[
              ['License plate', detailQuery.data.plate],
              ['Year', String(detailQuery.data.year)],
              ['Customer ID', detailQuery.data.customerId],
              ['VIN', detailQuery.data.vin ?? '-']
            ]}
          />
          <ServiceHistory records={historyQuery.data ?? []} loading={historyQuery.isLoading} />
        </Stack>
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
          <FormTextField control={control} name="customerId" label="Customer ID" />
          <FormTextField control={control} name="plate" label="License plate" />
          <FormTextField control={control} name="make" label="Brand" />
          <FormTextField control={control} name="model" label="Model" />
          <FormTextField control={control} name="year" label="Year" type="number" />
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
            <TableCell>Customer</TableCell>
            <TableCell>Phone</TableCell>
            <TableCell>Loyalty</TableCell>
            <TableCell align="right">Action</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {customers.map((customer) => (
            <TableRow key={customer.id} hover>
              <TableCell>{customer.name}</TableCell>
              <TableCell>{customer.phone}</TableCell>
              <TableCell>
                <Chip label={customer.loyaltyPoints} color="secondary" size="small" />
              </TableCell>
              <TableCell align="right">
                <Button component={RouterLink} to={`/customers/${customer.id}`}>
                  Details
                </Button>
              </TableCell>
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
            <TableCell>License plate</TableCell>
            <TableCell>Vehicle</TableCell>
            <TableCell>Year</TableCell>
            <TableCell align="right">Action</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {vehicles.map((vehicle) => (
            <TableRow key={vehicle.id} hover>
              <TableCell>{vehicle.plate}</TableCell>
              <TableCell>
                {vehicle.make} {vehicle.model}
              </TableCell>
              <TableCell>{vehicle.year}</TableCell>
              <TableCell align="right">
                <Button component={RouterLink} to={`/vehicles/${vehicle.id}`}>
                  Details
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Paper>
  );
}

function RelatedVehicles({ vehicles, loading }: { vehicles: Vehicle[]; loading: boolean }) {
  return (
    <Paper sx={{ p: { xs: 3, md: 4 } }}>
      <Typography variant="h3" sx={{ mb: 2 }}>
        Customer vehicles
      </Typography>
      {loading ? (
        <LoadingState />
      ) : vehicles.length ? (
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>License plate</TableCell>
              <TableCell>Vehicle</TableCell>
              <TableCell>Year</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {vehicles.map((vehicle) => (
              <TableRow key={vehicle.id}>
                <TableCell>{vehicle.plate}</TableCell>
                <TableCell>
                  {vehicle.make} {vehicle.model}
                </TableCell>
                <TableCell>{vehicle.year}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      ) : (
        <EmptyState />
      )}
    </Paper>
  );
}

function ServiceHistory({ records, loading }: { records: ServiceRecord[]; loading: boolean }) {
  return (
    <Paper sx={{ p: { xs: 3, md: 4 } }}>
      <Typography variant="h3" sx={{ mb: 2 }}>
        Service history
      </Typography>
      {loading ? (
        <LoadingState />
      ) : records.length ? (
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Date</TableCell>
              <TableCell>Service</TableCell>
              <TableCell>Mileage</TableCell>
              <TableCell align="right">Total</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {records.map((record) => (
              <TableRow key={record.id}>
                <TableCell>{record.performedAt}</TableCell>
                <TableCell>{record.summary}</TableCell>
                <TableCell>{record.mileage}</TableCell>
                <TableCell align="right">{record.cost}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      ) : (
        <EmptyState />
      )}
    </Paper>
  );
}

function DetailCard({ rows }: { rows: Array<[string, string]> }) {
  return (
    <Paper sx={{ p: { xs: 3, md: 4 } }}>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2 }}>
        {rows.map(([label, value]) => (
          <Box key={label} sx={{ p: 2, border: 1, borderColor: 'divider', borderRadius: 2 }}>
            <Typography variant="body2" color="text.secondary">
              {label}
            </Typography>
            <Tooltip title={value}>
              <Typography fontWeight={700} sx={{ overflowWrap: 'anywhere' }}>
                {value}
              </Typography>
            </Tooltip>
          </Box>
        ))}
      </Box>
      <Divider sx={{ mt: 3 }} />
    </Paper>
  );
}

function cleanFilters(filters: { firstName: string; lastName: string }) {
  return {
    firstName: filters.firstName.trim() || undefined,
    lastName: filters.lastName.trim() || undefined
  };
}

function cleanVehicleFilters(filters: { vin: string; plateNumber: string; owner: string }) {
  return {
    vin: filters.vin.trim() || undefined,
    plateNumber: filters.plateNumber.trim() || undefined,
    owner: filters.owner.trim() || undefined
  };
}

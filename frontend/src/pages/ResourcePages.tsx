import { useEffect, useState } from 'react';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { zodResolver } from '@hookform/resolvers/zod';
import {
  Autocomplete,
  Box,
  Button,
  CircularProgress,
  Divider,
  IconButton,
  InputAdornment,
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
import ClearRoundedIcon from '@mui/icons-material/ClearRounded';
import RestartAltRoundedIcon from '@mui/icons-material/RestartAltRounded';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import SearchRoundedIcon from '@mui/icons-material/SearchRounded';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Controller, useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { customersApi, vehiclesApi } from '../api/modules';
import { ApiErrorAlert, apiErrorMessage } from '../components/ApiErrorAlert';
import { FormTextField } from '../components/FormTextField';
import { EmptyState, ErrorState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';
import type { Customer, ServiceRecord, Vehicle } from '../types';
import { applyApiFieldErrors } from '../utils/apiFormErrors';

type Kind = 'customers' | 'vehicles';
type Mode = 'list' | 'create' | 'detail' | 'edit';

const customerSchema = z.object({
  name: z.string().min(1, 'Customer name is required'),
  phone: z.string().min(1, 'Phone is required'),
  email: z.string().email('Invalid email').optional().or(z.literal('')),
  notes: z.string().optional()
});

const vehicleSchema = z.object({
  customerId: z.string().min(1, 'Customer is required'),
  plate: z.string().min(1, 'License plate is required'),
  make: z.string().min(1, 'Brand is required'),
  model: z.string().min(1, 'Model is required'),
  year: z.coerce.number().min(1950),
  vin: z.string().optional(),
  fuelType: z.string().optional(),
  engine: z.string().optional()
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
  const [errorMessage, setErrorMessage] = useState('');
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
  const { control, handleSubmit, reset, setError, formState } = useForm<CustomerForm>({
    resolver: zodResolver(customerSchema) as never,
    defaultValues: { name: '', phone: '', email: '', notes: '' }
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
    if (listQuery.isError) return <ErrorState error={listQuery.error} />;
    return (
      <ResourceFrame title={t('customers')} actionLabel={t('newCustomer')} actionTo="/customers/new" actionIcon={<AddRoundedIcon />}>
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
              label={t('firstName')}
              value={filters.firstName}
              onChange={(event) => setFilters((current) => ({ ...current, firstName: event.target.value }))}
              fullWidth
            />
            <TextField
              name="lastName"
              label={t('lastName')}
              value={filters.lastName}
              onChange={(event) => setFilters((current) => ({ ...current, lastName: event.target.value }))}
              fullWidth
            />
            <Button type="submit" variant="outlined" startIcon={<SearchRoundedIcon />} sx={{ minWidth: 150 }}>
              {t('search')}
            </Button>
          </Stack>
        </Paper>
        <CustomerTable customers={listQuery.data ?? []} />
      </ResourceFrame>
    );
  }

  if (mode === 'detail') {
    if (detailQuery.isLoading) return <LoadingState />;
    if (detailQuery.isError) return <ErrorState error={detailQuery.error} />;
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
                try {
                  await deleteMutation.mutateAsync(id!);
                  await queryClient.invalidateQueries({ queryKey: ['customers'] });
                  showToast(t('deleted'));
                  navigate('/customers');
                } catch (error) {
                  showToast(apiErrorMessage(error, t('deleteFailed')), 'error');
                }
              }}
            >
              {t('delete')}
            </Button>
          </Stack>
          <DetailCard
            rows={[
              [t('phone'), detailQuery.data.phone],
              [t('email'), detailQuery.data.email ?? '-'],
              [t('address'), detailQuery.data.notes ?? '-']
            ]}
          />
          <RelatedVehicles vehicles={vehiclesQuery.data ?? []} loading={vehiclesQuery.isLoading} />
          <ServiceHistory records={historyQuery.data ?? []} loading={historyQuery.isLoading} />
        </Stack>
      </ResourceFrame>
    );
  }

  if (mode === 'edit' && detailQuery.isLoading) return <LoadingState />;
  if (mode === 'edit' && detailQuery.isError) return <ErrorState error={detailQuery.error} />;

  const onSubmit = handleSubmit(async (values) => {
    setErrorMessage('');
    try {
      const saved = mode === 'create' ? await createMutation.mutateAsync(values) : await updateMutation.mutateAsync(values);
      await queryClient.invalidateQueries({ queryKey: ['customers'] });
      showToast(t(mode === 'create' ? 'saved' : 'updated'));
      navigate(`/customers/${saved.id}`);
    } catch (error) {
      applyApiFieldErrors(error, setError, {
        firstName: 'name',
        lastName: 'name',
        fullName: 'name',
        address: 'notes'
      });
      setErrorMessage(apiErrorMessage(error, t('saveFailed')));
    }
  });

  return (
    <ResourceFrame title={mode === 'create' ? `${t('create')} ${t('customers')}` : `${t('edit')} ${detailQuery.data?.name ?? ''}`}>
      <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 } }}>
        <Box sx={{ mb: 3 }}>
          <ApiErrorAlert message={errorMessage} />
        </Box>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 3 }}>
          <FormTextField control={control} name="name" label={t('fullName')} />
          <FormTextField control={control} name="phone" label={t('phone')} />
          <FormTextField control={control} name="email" label={t('email')} />
          <FormTextField control={control} name="notes" label={t('address')} multiline minRows={3} sx={{ gridColumn: { md: '1 / -1' } }} />
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
  const [errorMessage, setErrorMessage] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [submittedSearchTerm, setSubmittedSearchTerm] = useState('');
  const listQuery = useQuery({
    queryKey: ['vehicles', submittedSearchTerm],
    queryFn: () => vehiclesApi.list(cleanVehicleSearch(submittedSearchTerm)),
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
  const customersQuery = useQuery({
    queryKey: ['customers', 'vehicle-picker'],
    queryFn: () => customersApi.list(),
    enabled: mode !== 'list'
  });
  const { control, handleSubmit, reset, setError, formState } = useForm<VehicleForm>({
    resolver: zodResolver(vehicleSchema) as never,
    defaultValues: { customerId: '', plate: '', make: '', model: '', year: new Date().getFullYear(), vin: '', fuelType: '', engine: '' }
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
    if (listQuery.isError) return <ErrorState error={listQuery.error} />;
    const resetSearch = () => {
      setSearchTerm('');
      setSubmittedSearchTerm('');
    };
    return (
      <ResourceFrame title={t('vehicles')} actionLabel={t('newVehicle')} actionTo="/vehicles/new" actionIcon={<AddRoundedIcon />}>
        <Paper sx={{ p: 2 }}>
          <Stack
            direction={{ xs: 'column', md: 'row' }}
            spacing={2}
            component="form"
            onSubmit={(event) => {
              event.preventDefault();
              setSubmittedSearchTerm(searchTerm);
            }}
          >
            <TextField
              name="vehicleSearch"
              label={t('search')}
              placeholder={t('searchVehiclePlaceholder')}
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
              InputProps={{
                endAdornment: searchTerm ? (
                  <InputAdornment position="end">
                    <IconButton aria-label={t('clearVehicleQuery')} edge="end" onClick={() => setSearchTerm('')}>
                      <ClearRoundedIcon />
                    </IconButton>
                  </InputAdornment>
                ) : null
              }}
              fullWidth
            />
            <Button type="submit" variant="outlined" startIcon={<SearchRoundedIcon />} sx={{ minWidth: 150 }}>
              {t('search')}
            </Button>
            <Button type="button" variant="text" startIcon={<RestartAltRoundedIcon />} onClick={resetSearch} sx={{ minWidth: 120 }}>
              {t('reset')}
            </Button>
          </Stack>
        </Paper>
        <VehicleTable vehicles={listQuery.data ?? []} searchTerm={submittedSearchTerm} />
      </ResourceFrame>
    );
  }

  if (mode === 'detail') {
    if (detailQuery.isLoading) return <LoadingState />;
    if (detailQuery.isError) return <ErrorState error={detailQuery.error} />;
    if (!detailQuery.data) return <EmptyState />;
    return (
      <ResourceFrame title={`${detailQuery.data.make} ${detailQuery.data.model}`} actionLabel={t('edit')} actionTo={`/vehicles/${id}/edit`}>
        <Stack spacing={3}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="flex-end">
            <Button component={RouterLink} to={`/services/new?customerId=${detailQuery.data.customerId}&vehicleId=${detailQuery.data.id}`} variant="contained" startIcon={<AddRoundedIcon />}>
              {t('newService')}
            </Button>
          </Stack>
          <DetailCard
            rows={[
              [t('licensePlate'), detailQuery.data.plate],
              [t('owner'), detailQuery.data.customerName ?? detailQuery.data.customerId],
              [t('year'), String(detailQuery.data.year)],
              [t('vin'), detailQuery.data.vin ?? '-'],
              [t('fuelType'), detailQuery.data.fuelType ?? '-'],
              [t('engine'), detailQuery.data.engine ?? '-']
            ]}
          />
          <ServiceHistory records={historyQuery.data ?? []} loading={historyQuery.isLoading} />
        </Stack>
      </ResourceFrame>
    );
  }

  if (mode === 'edit' && detailQuery.isLoading) return <LoadingState />;
  if (mode === 'edit' && detailQuery.isError) return <ErrorState error={detailQuery.error} />;

  const onSubmit = handleSubmit(async (values) => {
    setErrorMessage('');
    try {
      const saved = mode === 'create' ? await createMutation.mutateAsync(values) : await updateMutation.mutateAsync(values);
      await queryClient.invalidateQueries({ queryKey: ['vehicles'] });
      showToast(t(mode === 'create' ? 'saved' : 'updated'));
      navigate(`/vehicles/${saved.id}`);
    } catch (error) {
      applyApiFieldErrors(error, setError, {
        plateNumber: 'plate',
        modelYear: 'year'
      });
      setErrorMessage(apiErrorMessage(error, t('saveFailed')));
    }
  });

  return (
    <ResourceFrame title={mode === 'create' ? `${t('create')} ${t('vehicles')}` : `${t('edit')} ${detailQuery.data?.plate ?? ''}`}>
      <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 } }}>
        <Box sx={{ mb: 3 }}>
          <ApiErrorAlert message={errorMessage} />
        </Box>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 3 }}>
          <Controller
            control={control}
            name="customerId"
            render={({ field, fieldState }) => {
              const customers = customersQuery.data ?? [];
              const selectedCustomer = customers.find((customer) => String(customer.id) === field.value) ?? null;
              return (
                <Autocomplete
                  options={customers}
                  value={selectedCustomer}
                  loading={customersQuery.isLoading}
                  onChange={(_, value) => field.onChange(value ? String(value.id) : '')}
                  getOptionLabel={(customer) => customer.name}
                  isOptionEqualToValue={(option, value) => String(option.id) === String(value.id)}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label={t('customer')}
                      error={Boolean(fieldState.error)}
                      helperText={fieldState.error?.message}
                      InputProps={{
                        ...params.InputProps,
                        endAdornment: (
                          <>
                            {customersQuery.isLoading ? <CircularProgress color="inherit" size={20} /> : null}
                            {params.InputProps.endAdornment}
                          </>
                        )
                      }}
                    />
                  )}
                />
              );
            }}
          />
          <FormTextField control={control} name="plate" label={t('licensePlate')} />
          <FormTextField control={control} name="make" label={t('brand')} />
          <FormTextField control={control} name="model" label={t('model')} />
          <FormTextField control={control} name="year" label={t('year')} type="number" />
          <FormTextField control={control} name="vin" label={t('vin')} />
          <FormTextField control={control} name="fuelType" label={t('fuelType')} />
          <FormTextField control={control} name="engine" label={t('engine')} />
        </Box>
        <Button sx={{ mt: 3 }} type="submit" variant="contained" disabled={formState.isSubmitting}>
          {t('save')}
        </Button>
      </Paper>
    </ResourceFrame>
  );
}

function ResourceFrame({ title, children, actionLabel, actionTo, actionIcon }: { title: string; children: React.ReactNode; actionLabel?: string; actionTo?: string; actionIcon?: React.ReactNode }) {
  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }}>
        <Typography variant="h2">{title}</Typography>
        {actionTo && actionLabel ? (
          <Button component={RouterLink} to={actionTo} variant="contained" startIcon={actionIcon}>
            {actionLabel}
          </Button>
        ) : null}
      </Stack>
      {children}
    </Stack>
  );
}

function CustomerTable({ customers }: { customers: Customer[] }) {
  const { t } = useTranslation();
  if (!customers.length) return <EmptyState />;
  return (
    <Paper sx={{ overflow: 'auto' }}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>{t('customer')}</TableCell>
            <TableCell>{t('phone')}</TableCell>
            <TableCell align="right">{t('action')}</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {customers.map((customer) => (
            <TableRow key={customer.id} hover>
              <TableCell>{customer.name}</TableCell>
              <TableCell>{customer.phone}</TableCell>
              <TableCell align="right">
                <Button component={RouterLink} to={`/customers/${customer.id}`}>
                  {t('details')}
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Paper>
  );
}

function VehicleTable({ vehicles, searchTerm }: { vehicles: Vehicle[]; searchTerm: string }) {
  const { t } = useTranslation();
  if (!vehicles.length) return <EmptyState />;
  return (
    <Paper sx={{ overflow: 'auto' }}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell>{t('licensePlate')}</TableCell>
            <TableCell>{t('vin')}</TableCell>
            <TableCell>{t('owner')}</TableCell>
            <TableCell>{t('vehicle')}</TableCell>
            <TableCell>{t('year')}</TableCell>
            <TableCell>{t('fuel')}</TableCell>
            <TableCell>{t('engine')}</TableCell>
            <TableCell align="right">{t('action')}</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {vehicles.map((vehicle) => (
            <TableRow key={vehicle.id} hover>
              <TableCell><HighlightedText value={vehicle.plate} query={searchTerm} /></TableCell>
              <TableCell>
                <Tooltip title={vehicle.vin ?? '-'}>
                  <Box component="span">
                    <HighlightedText value={vehicle.vin ?? '-'} query={searchTerm} />
                  </Box>
                </Tooltip>
              </TableCell>
              <TableCell><HighlightedText value={vehicle.customerName ?? vehicle.customerId} query={searchTerm} /></TableCell>
              <TableCell>
                {vehicle.make} {vehicle.model}
              </TableCell>
              <TableCell>{vehicle.year}</TableCell>
              <TableCell>{vehicle.fuelType ?? '-'}</TableCell>
              <TableCell>{vehicle.engine ?? '-'}</TableCell>
              <TableCell align="right">
                <Button component={RouterLink} to={`/vehicles/${vehicle.id}`}>
                  {t('details')}
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Paper>
  );
}

function HighlightedText({ value, query }: { value: string; query: string }) {
  const trimmedQuery = query.trim();
  if (!trimmedQuery || !value) {
    return <>{value || '-'}</>;
  }
  const matchIndex = value.toLocaleLowerCase().indexOf(trimmedQuery.toLocaleLowerCase());
  if (matchIndex < 0) {
    return <>{value}</>;
  }
  const before = value.slice(0, matchIndex);
  const match = value.slice(matchIndex, matchIndex + trimmedQuery.length);
  const after = value.slice(matchIndex + trimmedQuery.length);
  return (
    <>
      {before}
      <Box component="mark" sx={{ bgcolor: 'secondary.light', color: 'text.primary', px: 0.35, borderRadius: 0.5 }}>
        {match}
      </Box>
      {after}
    </>
  );
}

function RelatedVehicles({ vehicles, loading }: { vehicles: Vehicle[]; loading: boolean }) {
  const { t } = useTranslation();
  return (
    <Paper sx={{ p: { xs: 3, md: 4 } }}>
      <Typography variant="h3" sx={{ mb: 2 }}>
        {t('customerVehicles')}
      </Typography>
      {loading ? (
        <LoadingState />
      ) : vehicles.length ? (
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>{t('licensePlate')}</TableCell>
              <TableCell>{t('vehicle')}</TableCell>
              <TableCell>{t('year')}</TableCell>
              <TableCell>{t('fuel')}</TableCell>
              <TableCell>{t('engine')}</TableCell>
              <TableCell align="right">{t('action')}</TableCell>
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
                <TableCell>{vehicle.fuelType ?? '-'}</TableCell>
                <TableCell>{vehicle.engine ?? '-'}</TableCell>
                <TableCell align="right">
                  <Button component={RouterLink} to={`/services/new?customerId=${vehicle.customerId}&vehicleId=${vehicle.id}`} startIcon={<AddRoundedIcon />}>
                    {t('newService')}
                  </Button>
                </TableCell>
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
  const { t } = useTranslation();
  return (
    <Paper sx={{ p: { xs: 3, md: 4 } }}>
      <Typography variant="h3" sx={{ mb: 2 }}>
        {t('serviceHistory')}
      </Typography>
      {loading ? (
        <LoadingState />
      ) : records.length ? (
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>{t('date')}</TableCell>
              <TableCell>{t('service')}</TableCell>
              <TableCell>{t('mileage')}</TableCell>
              <TableCell align="right">{t('total')}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {records.map((record) => (
              <TableRow key={record.id} hover component={RouterLink} to={`/services/${record.id}`} sx={{ cursor: 'pointer' }}>
                <TableCell>{record.performedAt}</TableCell>
                <TableCell>{record.summary}</TableCell>
                <TableCell>{record.mileage.toLocaleString('mk-MK')} km</TableCell>
                <TableCell align="right">{record.cost.toLocaleString('mk-MK')} ден.</TableCell>
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

function cleanVehicleSearch(query: string) {
  return {
    q: query.trim() || undefined
  };
}

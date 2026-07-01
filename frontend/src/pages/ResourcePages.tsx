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
import EditRoundedIcon from '@mui/icons-material/EditRounded';
import PersonRoundedIcon from '@mui/icons-material/PersonRounded';
import PhoneRoundedIcon from '@mui/icons-material/PhoneRounded';
import EmailRoundedIcon from '@mui/icons-material/EmailRounded';
import HomeRoundedIcon from '@mui/icons-material/HomeRounded';
import DirectionsCarRoundedIcon from '@mui/icons-material/DirectionsCarRounded';
import BadgeRoundedIcon from '@mui/icons-material/BadgeRounded';
import CalendarMonthRoundedIcon from '@mui/icons-material/CalendarMonthRounded';
import ConfirmationNumberRoundedIcon from '@mui/icons-material/ConfirmationNumberRounded';
import PinRoundedIcon from '@mui/icons-material/PinRounded';
import LocalGasStationRoundedIcon from '@mui/icons-material/LocalGasStationRounded';
import SettingsRoundedIcon from '@mui/icons-material/SettingsRounded';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Controller, useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { customersApi, vehiclesApi } from '../api/modules';
import { ApiErrorAlert, apiErrorMessage } from '../components/ApiErrorAlert';
import { FormDialog } from '../components/FormDialog';
import { FormTextField } from '../components/FormTextField';
import { ListPagination, useListPagination } from '../components/ListPagination';
import { EmptyState, ErrorState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';
import type { Customer, ServiceRecord, Vehicle } from '../types';
import { applyApiFieldErrors } from '../utils/apiFormErrors';

type Kind = 'customers' | 'vehicles';
type Mode = 'list' | 'detail' | 'edit';

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
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [submittedSearchTerm, setSubmittedSearchTerm] = useState('');
  const listQuery = useQuery({
    queryKey: ['customers', submittedSearchTerm],
    queryFn: () => customersApi.list(cleanCustomerSearch(submittedSearchTerm)),
    enabled: mode === 'list'
  });
  const detailQuery = useQuery({
    queryKey: ['customers', id],
    queryFn: () => customersApi.get(id!),
    enabled: Boolean(id) && mode !== 'list'
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

  const onSubmit = handleSubmit(async (values) => {
    setErrorMessage('');
    try {
      const saved = id ? await updateMutation.mutateAsync(values) : await createMutation.mutateAsync(values);
      await queryClient.invalidateQueries({ queryKey: ['customers'] });
      showToast(t(id ? 'updated' : 'saved'));
      setEditDialogOpen(false);
      setCreateDialogOpen(false);
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

  const openCreateDialog = () => {
    setErrorMessage('');
    reset({ name: '', phone: '', email: '', notes: '' });
    setCreateDialogOpen(true);
  };

  const formTitle = `${t('edit')} ${detailQuery.data?.name ?? ''}`;
  const customerForm = (
    <>
      <ApiErrorAlert message={errorMessage} />
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, minmax(0, 1fr))' },
          gap: 2
        }}
      >
        <FormTextField control={control} name="name" label={t('fullName')} placeholder="Александар Стојановски" autoComplete="name" InputProps={{ startAdornment: fieldIcon(<PersonRoundedIcon />) }} sx={{ gridColumn: { sm: '1 / -1' } }} />
        <FormTextField control={control} name="phone" label={t('phone')} placeholder="+389 70 123 456" autoComplete="tel" InputProps={{ startAdornment: fieldIcon(<PhoneRoundedIcon />) }} />
        <FormTextField control={control} name="email" label={t('email')} placeholder="korisnik@email.com" type="email" autoComplete="email" InputProps={{ startAdornment: fieldIcon(<EmailRoundedIcon />) }} />
        <FormTextField control={control} name="notes" label={t('address')} placeholder="ул. Пример 1, Скопје" autoComplete="street-address" InputProps={{ startAdornment: fieldIcon(<HomeRoundedIcon />) }} sx={{ gridColumn: { sm: '1 / -1' } }} />
      </Box>
    </>
  );

  if (mode === 'list') {
    if (listQuery.isLoading) return <LoadingState />;
    if (listQuery.isError) return <ErrorState error={listQuery.error} />;
    const resetSearch = () => {
      setSearchTerm('');
      setSubmittedSearchTerm('');
    };
    return (
      <>
      <ResourceFrame title={t('customers')} actionLabel={t('newCustomer')} onAction={openCreateDialog} actionIcon={<AddRoundedIcon />}>
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
              name="customerSearch"
              label={t('search')}
              placeholder={t('searchCustomerPlaceholder')}
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
              InputProps={{
                endAdornment: searchTerm ? (
                  <InputAdornment position="end">
                    <IconButton aria-label={t('clearCustomerQuery')} edge="end" onClick={() => setSearchTerm('')}>
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
        <CustomerTable customers={listQuery.data ?? []} searchTerm={submittedSearchTerm} />
      </ResourceFrame>
        {createDialogOpen ? (
          <FormDialog
            title={t('newCustomer')}
            onClose={() => setCreateDialogOpen(false)}
            onSubmit={onSubmit}
            isSubmitting={formState.isSubmitting}
          >
            {customerForm}
          </FormDialog>
        ) : null}
      </>
    );
  }

  if (mode === 'edit' && detailQuery.isLoading) return <LoadingState />;
  if (mode === 'edit' && detailQuery.isError) return <ErrorState error={detailQuery.error} />;

  if (mode === 'detail') {
    if (detailQuery.isLoading) return <LoadingState />;
    if (detailQuery.isError) return <ErrorState error={detailQuery.error} />;
    if (!detailQuery.data) return <EmptyState />;
    return (
      <>
        <ResourceFrame title={detailQuery.data.name}>
          <Stack spacing={3}>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="flex-end">
              <Button variant="contained" startIcon={<EditRoundedIcon />} onClick={() => setEditDialogOpen(true)}>
                {t('edit')}
              </Button>
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
        {editDialogOpen ? (
          <FormDialog
            title={`${t('edit')} ${detailQuery.data.name}`}
            onClose={() => setEditDialogOpen(false)}
            onSubmit={onSubmit}
            isSubmitting={formState.isSubmitting}
          >
            {customerForm}
          </FormDialog>
        ) : null}
      </>
    );
  }

  if (mode === 'edit' && detailQuery.data) {
    return (
      <>
        <ResourceFrame title={detailQuery.data.name}>
          <DetailCard
            rows={[
              [t('phone'), detailQuery.data.phone],
              [t('email'), detailQuery.data.email ?? '-'],
              [t('address'), detailQuery.data.notes ?? '-']
            ]}
          />
        </ResourceFrame>
        <FormDialog
          title={formTitle}
          onClose={() => navigate(`/customers/${id}`)}
          onSubmit={onSubmit}
          isSubmitting={formState.isSubmitting}
        >
          {customerForm}
        </FormDialog>
      </>
    );
  }

  return <LoadingState />;
}

function VehiclePage({ mode }: { mode: Mode }) {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const [errorMessage, setErrorMessage] = useState('');
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
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
    enabled: Boolean(id) && mode !== 'list'
  });
  const historyQuery = useQuery({
    queryKey: ['vehicles', id, 'service-history'],
    queryFn: () => vehiclesApi.serviceHistory(id!),
    enabled: Boolean(id) && mode === 'detail'
  });
  const customersQuery = useQuery({
    queryKey: ['customers', 'vehicle-picker'],
    queryFn: () => customersApi.list(),
    enabled: mode !== 'list' || createDialogOpen
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

  const onSubmit = handleSubmit(async (values) => {
    setErrorMessage('');
    try {
      const saved = id ? await updateMutation.mutateAsync(values) : await createMutation.mutateAsync(values);
      await queryClient.invalidateQueries({ queryKey: ['vehicles'] });
      showToast(t(id ? 'updated' : 'saved'));
      setEditDialogOpen(false);
      setCreateDialogOpen(false);
      navigate(`/vehicles/${saved.id}`);
    } catch (error) {
      applyApiFieldErrors(error, setError, {
        plateNumber: 'plate',
        modelYear: 'year'
      });
      setErrorMessage(apiErrorMessage(error, t('saveFailed')));
    }
  });

  const openCreateDialog = () => {
    setErrorMessage('');
    reset({ customerId: '', plate: '', make: '', model: '', year: new Date().getFullYear(), vin: '', fuelType: '', engine: '' });
    setCreateDialogOpen(true);
  };

  const formTitle = `${t('edit')} ${detailQuery.data?.plate ?? ''}`;
  const vehicleForm = (
    <>
      <ApiErrorAlert message={errorMessage} />
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)' }, gap: 2 }}>
        <Box sx={{ gridColumn: { sm: '1 / -1' } }}>
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
                        startAdornment: (
                          <>
                            {fieldIcon(<PersonRoundedIcon />)}
                            {params.InputProps.startAdornment}
                          </>
                        ),
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
        </Box>
        <FormTextField control={control} name="make" label={t('brand')} placeholder="BMW" InputProps={{ startAdornment: fieldIcon(<DirectionsCarRoundedIcon />) }} />
        <FormTextField control={control} name="model" label={t('model')} placeholder="320d" InputProps={{ startAdornment: fieldIcon(<BadgeRoundedIcon />) }} />
        <FormTextField control={control} name="year" label={t('year')} placeholder="2022" type="number" InputProps={{ startAdornment: fieldIcon(<CalendarMonthRoundedIcon />) }} />
        <FormTextField control={control} name="plate" label={t('licensePlate')} placeholder="SK 1234 AB" InputProps={{ startAdornment: fieldIcon(<ConfirmationNumberRoundedIcon />) }} />
        <FormTextField control={control} name="vin" label={t('vin')} placeholder="WBA3A5G5XFNS12345" InputProps={{ startAdornment: fieldIcon(<PinRoundedIcon />) }} sx={{ gridColumn: { sm: '1 / -1' } }} />
        <FormTextField control={control} name="fuelType" label={t('fuelType')} placeholder="Дизел" InputProps={{ startAdornment: fieldIcon(<LocalGasStationRoundedIcon />) }} />
        <FormTextField control={control} name="engine" label={t('engine')} placeholder="2.0 TDI" InputProps={{ startAdornment: fieldIcon(<SettingsRoundedIcon />) }} />
      </Box>
    </>
  );

  if (mode === 'list') {
    if (listQuery.isLoading) return <LoadingState />;
    if (listQuery.isError) return <ErrorState error={listQuery.error} />;
    const resetSearch = () => {
      setSearchTerm('');
      setSubmittedSearchTerm('');
    };
    return (
      <>
      <ResourceFrame title={t('vehicles')} actionLabel={t('newVehicle')} onAction={openCreateDialog} actionIcon={<AddRoundedIcon />}>
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
        {createDialogOpen ? (
          <FormDialog
            title={t('newVehicle')}
            onClose={() => setCreateDialogOpen(false)}
            onSubmit={onSubmit}
            isSubmitting={formState.isSubmitting}
          >
            {vehicleForm}
          </FormDialog>
        ) : null}
      </>
    );
  }

  if (mode === 'edit' && detailQuery.isLoading) return <LoadingState />;
  if (mode === 'edit' && detailQuery.isError) return <ErrorState error={detailQuery.error} />;

  if (mode === 'detail') {
    if (detailQuery.isLoading) return <LoadingState />;
    if (detailQuery.isError) return <ErrorState error={detailQuery.error} />;
    if (!detailQuery.data) return <EmptyState />;
    return (
      <>
        <ResourceFrame title={`${detailQuery.data.make} ${detailQuery.data.model}`}>
          <Stack spacing={3}>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="flex-end">
              <Button variant="outlined" startIcon={<EditRoundedIcon />} onClick={() => setEditDialogOpen(true)}>
                {t('edit')}
              </Button>
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
        {editDialogOpen ? (
          <FormDialog
            title={`${t('edit')} ${detailQuery.data.plate}`}
            onClose={() => setEditDialogOpen(false)}
            onSubmit={onSubmit}
            isSubmitting={formState.isSubmitting}
          >
            {vehicleForm}
          </FormDialog>
        ) : null}
      </>
    );
  }

  if (mode === 'edit' && detailQuery.data) {
    return (
      <>
        <ResourceFrame title={`${detailQuery.data.make} ${detailQuery.data.model}`}>
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
        </ResourceFrame>
        <FormDialog
          title={formTitle}
          onClose={() => navigate(`/vehicles/${id}`)}
          onSubmit={onSubmit}
          isSubmitting={formState.isSubmitting}
        >
          {vehicleForm}
        </FormDialog>
      </>
    );
  }

  return <LoadingState />;
}

function fieldIcon(icon: React.ReactNode) {
  return (
    <InputAdornment position="start" sx={{ color: 'text.secondary' }}>
      {icon}
    </InputAdornment>
  );
}

function ResourceFrame({ title, children, actionLabel, onAction, actionIcon }: { title: string; children: React.ReactNode; actionLabel?: string; onAction?: () => void; actionIcon?: React.ReactNode }) {
  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }}>
        <Typography variant="h2">{title}</Typography>
        {onAction && actionLabel ? (
          <Button onClick={onAction} variant="contained" startIcon={actionIcon}>
            {actionLabel}
          </Button>
        ) : null}
      </Stack>
      {children}
    </Stack>
  );
}

function CustomerTable({ customers, searchTerm }: { customers: Customer[]; searchTerm: string }) {
  const { t } = useTranslation();
  const pagination = useListPagination(customers);
  if (!customers.length) return <EmptyState />;
  return (
    <Paper sx={{ overflow: 'hidden' }}>
      <Box sx={{ overflow: 'auto' }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>{t('customer')}</TableCell>
              <TableCell>{t('phone')}</TableCell>
              <TableCell>{t('email')}</TableCell>
              <TableCell align="right">{t('action')}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {pagination.pageItems.map((customer) => (
              <TableRow key={customer.id} hover>
                <TableCell><HighlightedText value={customer.name} query={searchTerm} /></TableCell>
                <TableCell><HighlightedText value={customer.phone} query={searchTerm} /></TableCell>
                <TableCell><HighlightedText value={customer.email ?? '-'} query={searchTerm} /></TableCell>
                <TableCell align="right">
                  <Button component={RouterLink} to={`/customers/${customer.id}`}>
                    {t('details')}
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </Box>
      <ListPagination
        page={pagination.page}
        pageCount={pagination.pageCount}
        pageSize={pagination.pageSize}
        totalItems={pagination.totalItems}
        onPageChange={pagination.setPage}
        onPageSizeChange={pagination.setPageSize}
      />
    </Paper>
  );
}

function VehicleTable({ vehicles, searchTerm }: { vehicles: Vehicle[]; searchTerm: string }) {
  const { t } = useTranslation();
  const pagination = useListPagination(vehicles);
  if (!vehicles.length) return <EmptyState />;
  return (
    <Paper sx={{ overflow: 'hidden' }}>
      <Box sx={{ overflow: 'auto' }}>
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
            {pagination.pageItems.map((vehicle) => (
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
      </Box>
      <ListPagination
        page={pagination.page}
        pageCount={pagination.pageCount}
        pageSize={pagination.pageSize}
        totalItems={pagination.totalItems}
        onPageChange={pagination.setPage}
        onPageSizeChange={pagination.setPageSize}
      />
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
  const pagination = useListPagination(vehicles, 5);
  return (
    <Paper sx={{ p: { xs: 3, md: 4 } }}>
      <Typography variant="h3" sx={{ mb: 2 }}>
        {t('customerVehicles')}
      </Typography>
      {loading ? (
        <LoadingState />
      ) : vehicles.length ? (
        <>
          <Box sx={{ overflow: 'auto' }}>
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
                {pagination.pageItems.map((vehicle) => (
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
          </Box>
          <ListPagination
            page={pagination.page}
            pageCount={pagination.pageCount}
            pageSize={pagination.pageSize}
            totalItems={pagination.totalItems}
            onPageChange={pagination.setPage}
            onPageSizeChange={pagination.setPageSize}
          />
        </>
      ) : (
        <EmptyState />
      )}
    </Paper>
  );
}

function ServiceHistory({ records, loading }: { records: ServiceRecord[]; loading: boolean }) {
  const { t } = useTranslation();
  const pagination = useListPagination(records, 5);
  return (
    <Paper sx={{ p: { xs: 3, md: 4 } }}>
      <Typography variant="h3" sx={{ mb: 2 }}>
        {t('serviceHistory')}
      </Typography>
      {loading ? (
        <LoadingState />
      ) : records.length ? (
        <>
          <Box sx={{ overflow: 'auto' }}>
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
                {pagination.pageItems.map((record) => (
                  <TableRow key={record.id} hover component={RouterLink} to={`/services/${record.id}`} sx={{ cursor: 'pointer' }}>
                    <TableCell>{record.performedAt}</TableCell>
                    <TableCell>{record.summary}</TableCell>
                    <TableCell>{record.mileage.toLocaleString('mk-MK')} km</TableCell>
                    <TableCell align="right">{record.cost.toLocaleString('mk-MK')} ден.</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Box>
          <ListPagination
            page={pagination.page}
            pageCount={pagination.pageCount}
            pageSize={pagination.pageSize}
            totalItems={pagination.totalItems}
            onPageChange={pagination.setPage}
            onPageSizeChange={pagination.setPageSize}
          />
        </>
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

function cleanCustomerSearch(query: string) {
  return {
    q: query.trim() || undefined
  };
}

function cleanVehicleSearch(query: string) {
  return {
    q: query.trim() || undefined
  };
}

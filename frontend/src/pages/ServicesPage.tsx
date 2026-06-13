import { zodResolver } from '@hookform/resolvers/zod';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import DeleteRoundedIcon from '@mui/icons-material/DeleteRounded';
import {
  Autocomplete,
  Box,
  Button,
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
  Typography
} from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Controller, useFieldArray, useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink, useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { z } from 'zod';
import { customersApi, serviceRecordsApi } from '../api/modules';
import { DateInput, TimeInput } from '../components/DateTimeInputs';
import { FormTextField } from '../components/FormTextField';
import { EmptyState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';
import type { Customer, Vehicle } from '../types';
import { skopjeDate, skopjeTime } from '../utils/dateTime';

const schema = z.object({
  customerId: z.string().min(1),
  vehicleId: z.string().min(1),
  performedAt: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
  serviceTime: z.string().regex(/^\d{2}:\d{2}$/),
  mileage: z.coerce.number().min(0),
  summary: z.string().min(1),
  laborCost: z.coerce.number().min(0),
  parts: z.array(z.object({ name: z.string().min(1), price: z.coerce.number().min(0) })),
  notes: z.string().optional()
});

type ServiceForm = z.output<typeof schema>;

const customerLabel = (customer: Customer | null) => customer?.name ?? '';
const vehicleLabel = (vehicle: Vehicle | null) =>
  vehicle ? `${vehicle.plate} - ${vehicle.make} ${vehicle.model}${vehicle.year ? ` (${vehicle.year})` : ''}` : '';
const sumParts = (parts: Array<{ price?: number | string }>) => parts.reduce((total, part) => total + (Number(part.price) || 0), 0);

export function ServicesPage({ mode = 'list' }: { mode?: 'list' | 'create' | 'detail' }) {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const listQuery = useQuery({ queryKey: ['service-records'], queryFn: serviceRecordsApi.list, enabled: mode === 'list' });
  const detailQuery = useQuery({ queryKey: ['service-records', id], queryFn: () => serviceRecordsApi.get(id!), enabled: mode === 'detail' && Boolean(id) });
  const createMutation = useMutation({ mutationFn: serviceRecordsApi.create });
  const { clearErrors, control, handleSubmit, setValue, formState } = useForm<ServiceForm>({
    resolver: zodResolver(schema) as never,
    defaultValues: {
      customerId: searchParams.get('customerId') ?? '',
      vehicleId: searchParams.get('vehicleId') ?? '',
      performedAt: skopjeDate(new Date()),
      serviceTime: skopjeTime(new Date()),
      mileage: 0,
      summary: 'Minor Service',
      laborCost: 0,
      parts: [],
      notes: ''
    }
  });
  const { fields: partFields, append: appendPart, remove: removePart } = useFieldArray({ control, name: 'parts' });
  const selectedCustomerId = useWatch({ control, name: 'customerId' });
  const selectedVehicleId = useWatch({ control, name: 'vehicleId' });
  const watchedParts = useWatch({ control, name: 'parts' }) ?? [];
  const watchedLaborCost = useWatch({ control, name: 'laborCost' });
  const customersQuery = useQuery({ queryKey: ['customers', 'service-form'], queryFn: () => customersApi.list(), enabled: mode === 'create' });
  const vehiclesQuery = useQuery({
    queryKey: ['customers', selectedCustomerId, 'vehicles'],
    queryFn: () => customersApi.vehicles(selectedCustomerId),
    enabled: mode === 'create' && Boolean(selectedCustomerId)
  });
  const customers = customersQuery.data ?? [];
  const vehicles = vehiclesQuery.data ?? [];
  const selectedCustomer = customers.find((customer) => String(customer.id) === selectedCustomerId) ?? null;
  const selectedVehicle = vehicles.find((vehicle) => String(vehicle.id) === selectedVehicleId) ?? null;
  const partsCost = sumParts(watchedParts);
  const totalCost = partsCost + (Number(watchedLaborCost) || 0);

  if (mode === 'create') {
    const onSubmit = handleSubmit(async (values) => {
      const { parts, serviceTime: _serviceTime, ...serviceValues } = values;
      void _serviceTime;
      const replacedParts = parts.map((part) => `${part.name} (${Number(part.price).toLocaleString('mk-MK')} den.)`).join(', ');
      const computedPartsCost = sumParts(parts);
      await createMutation.mutateAsync({
        ...serviceValues,
        replacedParts,
        partsCost: computedPartsCost,
        cost: computedPartsCost + values.laborCost
      });
      await queryClient.invalidateQueries({ queryKey: ['service-records'] });
      showToast(t('saved'));
      navigate('/services');
    });

    return (
      <Stack spacing={3}>
        <Typography variant="h2">{t('newService')}</Typography>
        <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 } }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 3 }}>
            <Controller
              control={control}
              name="customerId"
              render={({ field, fieldState }) => (
                <Autocomplete
                  options={customers}
                  value={selectedCustomer}
                  loading={customersQuery.isLoading}
                  getOptionLabel={customerLabel}
                  isOptionEqualToValue={(option, value) => String(option.id) === String(value.id)}
                  onChange={(_, customer) => {
                    field.onChange(customer ? String(customer.id) : '');
                    setValue('vehicleId', '', { shouldDirty: true, shouldValidate: false });
                    clearErrors('vehicleId');
                  }}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label={t('customer')}
                      inputRef={field.ref}
                      name={field.name}
                      onBlur={field.onBlur}
                      error={Boolean(fieldState.error)}
                      helperText={fieldState.error?.message}
                    />
                  )}
                />
              )}
            />
            <Controller
              control={control}
              name="vehicleId"
              render={({ field, fieldState }) => (
                <Autocomplete
                  options={vehicles}
                  value={selectedVehicle}
                  loading={vehiclesQuery.isLoading}
                  disabled={!selectedCustomerId}
                  getOptionLabel={vehicleLabel}
                  isOptionEqualToValue={(option, value) => String(option.id) === String(value.id)}
                  onChange={(_, vehicle) => {
                    field.onChange(vehicle ? String(vehicle.id) : '');
                    if (vehicle) {
                      clearErrors('vehicleId');
                    }
                  }}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label={t('vehicle')}
                      inputRef={field.ref}
                      name={field.name}
                      onBlur={field.onBlur}
                      error={Boolean(fieldState.error)}
                      helperText={fieldState.error?.message ?? (!selectedCustomerId ? t('selectCustomerFirst') : undefined)}
                    />
                  )}
                />
              )}
            />
            <Controller
              control={control}
              name="performedAt"
              render={({ field, fieldState }) => (
                <DateInput
                  name={field.name}
                  label={t('date')}
                  value={field.value}
                  helperText={fieldState.error?.message ?? 'DD.MM.YYYY'}
                  error={Boolean(fieldState.error)}
                  onBlur={field.onBlur}
                  onChange={field.onChange}
                  inputRef={field.ref}
                />
              )}
            />
            <Controller
              control={control}
              name="serviceTime"
              render={({ field, fieldState }) => (
                <TimeInput
                  name={field.name}
                  label={t('time')}
                  value={field.value}
                  helperText={fieldState.error?.message ?? 'HH:mm'}
                  error={Boolean(fieldState.error)}
                  onBlur={field.onBlur}
                  onChange={field.onChange}
                  inputRef={field.ref}
                />
              )}
            />
            <FormTextField
              control={control}
              name="mileage"
              label={t('mileage')}
              type="number"
              InputProps={{ endAdornment: <InputAdornment position="end">km</InputAdornment> }}
            />
            <FormTextField control={control} name="summary" label={t('serviceType')} />
            <Box sx={{ gridColumn: { md: '1 / -1' } }}>
              <Stack spacing={2}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="space-between" alignItems={{ xs: 'stretch', sm: 'center' }}>
                  <Typography variant="h4">{t('replacedParts')}</Typography>
                  <Button type="button" variant="outlined" startIcon={<AddRoundedIcon />} onClick={() => appendPart({ name: '', price: 0 })}>
                    {t('addPart')}
                  </Button>
                </Stack>
                {partFields.length ? (
                  <Stack spacing={1.5}>
                    {partFields.map((part, index) => (
                      <Box key={part.id} sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 180px auto' }, gap: 1.5, alignItems: 'start' }}>
                        <FormTextField control={control} name={`parts.${index}.name`} label={t('partName')} />
                        <FormTextField
                          control={control}
                          name={`parts.${index}.price`}
                          label={t('price')}
                          type="number"
                          InputProps={{ endAdornment: <InputAdornment position="end">den.</InputAdornment> }}
                        />
                        <IconButton aria-label={`${t('delete')} ${index + 1}`} onClick={() => removePart(index)} sx={{ mt: { sm: 1 } }}>
                          <DeleteRoundedIcon />
                        </IconButton>
                      </Box>
                    ))}
                  </Stack>
                ) : (
                  <Typography color="text.secondary">{t('noReplacedParts')}</Typography>
                )}
                <Divider />
                <Typography fontWeight={700}>{t('partsTotal', { amount: partsCost.toLocaleString('mk-MK') })}</Typography>
              </Stack>
            </Box>
            <FormTextField
              control={control}
              name="laborCost"
              label={t('laborCost')}
              type="number"
              InputProps={{ endAdornment: <InputAdornment position="end">den.</InputAdornment> }}
            />
            <TextField label={t('total')} value={`${totalCost.toLocaleString('mk-MK')} den.`} InputProps={{ readOnly: true }} />
            <FormTextField control={control} name="notes" label={t('notes')} multiline minRows={4} sx={{ gridColumn: { md: '1 / -1' } }} />
          </Box>
          <Button sx={{ mt: 3 }} type="submit" variant="contained" disabled={formState.isSubmitting}>
            {t('save')}
          </Button>
        </Paper>
      </Stack>
    );
  }

  if (mode === 'detail') {
    if (detailQuery.isLoading) return <LoadingState />;
    const record = detailQuery.data;
    if (!record) return <EmptyState />;

    return (
      <Stack spacing={3}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }}>
          <Box>
            <Typography variant="h2">{record.summary}</Typography>
            <Typography color="text.secondary">
              {record.performedAt} - {record.cost.toLocaleString('mk-MK')} den.
            </Typography>
          </Box>
          <Stack direction="row" spacing={1}>
            <Button component={RouterLink} to={`/vehicles/${record.vehicleId}`} variant="outlined">
              {t('vehicle')}
            </Button>
            <Button component={RouterLink} to={`/customers/${record.customerId}`} variant="outlined">
              {t('customer')}
            </Button>
          </Stack>
        </Stack>
        <Paper sx={{ p: { xs: 3, md: 4 } }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2 }}>
            <ServiceField label={t('customerId')} value={record.customerId} />
            <ServiceField label={t('vehicleId')} value={record.vehicleId} />
            <ServiceField label={t('serviceDate')} value={record.performedAt} />
            <ServiceField label={t('serviceType')} value={record.summary} />
            <ServiceField label={t('mileage')} value={`${record.mileage.toLocaleString('mk-MK')} km`} />
            <ServiceField label={t('replacedParts')} value={record.replacedParts || '-'} wide />
            <ServiceField label={t('partsCost')} value={`${record.partsCost.toLocaleString('mk-MK')} den.`} />
            <ServiceField label={t('laborCost')} value={`${record.laborCost.toLocaleString('mk-MK')} den.`} />
            <ServiceField label={t('total')} value={`${record.cost.toLocaleString('mk-MK')} den.`} />
            <ServiceField label={t('notes')} value={record.notes || '-'} wide />
          </Box>
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
                <TableCell>{t('vehicle')}</TableCell>
                <TableCell>{t('date')}</TableCell>
                <TableCell>{t('service')}</TableCell>
                <TableCell align="right">{t('parts')}</TableCell>
                <TableCell align="right">{t('labor')}</TableCell>
                <TableCell align="right">{t('total')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {records.map((record) => (
                <TableRow key={record.id} hover component={RouterLink} to={`/services/${record.id}`} sx={{ cursor: 'pointer' }}>
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

function ServiceField({ label, value, wide = false }: { label: string; value: string; wide?: boolean }) {
  return (
    <Box sx={{ p: 2, border: 1, borderColor: 'divider', borderRadius: 2, gridColumn: wide ? { md: '1 / -1' } : undefined }}>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography fontWeight={700} sx={{ overflowWrap: 'anywhere' }}>
        {value}
      </Typography>
    </Box>
  );
}

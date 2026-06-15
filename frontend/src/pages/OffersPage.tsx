import AddRoundedIcon from '@mui/icons-material/AddRounded';
import DeleteRoundedIcon from '@mui/icons-material/DeleteRounded';
import { zodResolver } from '@hookform/resolvers/zod';
import { Alert, Autocomplete, Box, Button, Chip, CircularProgress, Divider, IconButton, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Controller, useFieldArray, useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { z } from 'zod';
import { customersApi, offersApi } from '../api/modules';
import { FormTextField } from '../components/FormTextField';
import { EmptyState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';
import type { Customer, Offer, OfferPart, Vehicle } from '../types';
import { downloadBlob } from '../utils/download';

const partSchema = z.object({
  name: z.string().min(1),
  price: z.coerce.number().min(0)
});

const schema = z.object({
  customerId: z.string().min(1),
  vehicleId: z.string().optional(),
  title: z.string().min(1),
  parts: z.array(partSchema),
  laborCost: z.coerce.number().min(0),
  status: z.literal('DRAFT')
});

type OfferForm = z.output<typeof schema>;

export function OffersPage({ mode = 'list' }: { mode?: 'list' | 'create' | 'detail' }) {
  const { t } = useTranslation();
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const listQuery = useQuery({ queryKey: ['offers'], queryFn: offersApi.list, enabled: mode === 'list' });
  const detailQuery = useQuery({ queryKey: ['offers', id], queryFn: () => offersApi.get(id!), enabled: mode === 'detail' && Boolean(id) });
  const createMutation = useMutation({ mutationFn: offersApi.create });
  const sendMutation = useMutation({ mutationFn: offersApi.send });
  const exportMutation = useMutation({ mutationFn: offersApi.exportPdf });
  const { clearErrors, control, handleSubmit, setValue, formState } = useForm<OfferForm>({
    resolver: zodResolver(schema) as never,
    defaultValues: { customerId: '', vehicleId: '', title: '', parts: [], laborCost: 0, status: 'DRAFT' }
  });
  const { fields: partFields, append: appendPart, remove: removePart } = useFieldArray({ control, name: 'parts' });
  const selectedCustomerId = useWatch({ control, name: 'customerId' });
  const watchedParts = useWatch({ control, name: 'parts' }) ?? [];
  const watchedLaborCost = useWatch({ control, name: 'laborCost' });
  const customersQuery = useQuery({ queryKey: ['customers', 'offer-form'], queryFn: () => customersApi.list(), enabled: mode === 'create' });
  const vehiclesQuery = useQuery({
    queryKey: ['customers', selectedCustomerId, 'vehicles', 'offer-form'],
    queryFn: () => customersApi.vehicles(selectedCustomerId),
    enabled: mode === 'create' && Boolean(selectedCustomerId)
  });
  const loyaltyQuery = useQuery({
    queryKey: ['customers', selectedCustomerId, 'loyalty-status'],
    queryFn: () => customersApi.loyaltyStatus(selectedCustomerId),
    enabled: mode === 'create' && Boolean(selectedCustomerId)
  });
  const partsCost = sumParts(watchedParts);
  const laborCost = Number(watchedLaborCost) || 0;
  const subtotalCost = partsCost + laborCost;
  const discountPercent = loyaltyQuery.data?.discountPercent ?? 0;
  const discountAmount = loyaltyQuery.data?.loyal ? Math.round((subtotalCost * discountPercent)) / 100 : 0;
  const totalCost = subtotalCost - discountAmount;

  if (mode === 'create') {
    const onSubmit = handleSubmit(async (values) => {
      const parts = values.parts.map((part) => ({ name: part.name.trim(), price: Number(part.price) }));
      const computedPartsCost = sumParts(parts);
      const created = await createMutation.mutateAsync({
        customerId: values.customerId,
        vehicleId: values.vehicleId || undefined,
        title: values.title,
        parts,
        partsCost: computedPartsCost,
        laborCost: Number(values.laborCost) || 0,
        subtotal: computedPartsCost + (Number(values.laborCost) || 0),
        discountPercent: 0,
        discountAmount: 0,
        total: computedPartsCost + (Number(values.laborCost) || 0),
        status: 'DRAFT'
      });
      await queryClient.invalidateQueries({ queryKey: ['offers'] });
      showToast(t('offerSent'));
      navigate(`/offers/${created.id}`);
    });

    return (
      <Stack spacing={3}>
        <Typography variant="h2">{t('createOffer')}</Typography>
        <Box component="form" onSubmit={onSubmit} sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 1fr) 360px' }, gap: 3, alignItems: 'start' }}>
          <Paper sx={{ p: { xs: 3, md: 4 } }}>
            <Stack spacing={3}>
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
                        getOptionLabel={customerLabel}
                        isOptionEqualToValue={(option, value) => String(option.id) === String(value.id)}
                        onChange={(_, value) => {
                          field.onChange(value ? String(value.id) : '');
                          setValue('vehicleId', '');
                          clearErrors('customerId');
                        }}
                        renderInput={(params) => (
                          <TextField
                            {...params}
                            label={t('customer')}
                            inputRef={field.ref}
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
                <Controller
                  control={control}
                  name="vehicleId"
                  render={({ field, fieldState }) => {
                    const vehicles = vehiclesQuery.data ?? [];
                    const selectedVehicle = vehicles.find((vehicle) => String(vehicle.id) === field.value) ?? null;
                    return (
                      <Autocomplete
                        options={vehicles}
                        value={selectedVehicle}
                        loading={vehiclesQuery.isLoading}
                        disabled={!selectedCustomerId}
                        getOptionLabel={vehicleLabel}
                        isOptionEqualToValue={(option, value) => String(option.id) === String(value.id)}
                        onChange={(_, value) => {
                          field.onChange(value ? String(value.id) : '');
                          clearErrors('vehicleId');
                        }}
                        renderInput={(params) => (
                          <TextField
                            {...params}
                            label={t('vehicle')}
                            inputRef={field.ref}
                            error={Boolean(fieldState.error)}
                            helperText={fieldState.error?.message ?? (!selectedCustomerId ? t('selectCustomerFirst') : undefined)}
                            InputProps={{
                              ...params.InputProps,
                              endAdornment: (
                                <>
                                  {vehiclesQuery.isLoading ? <CircularProgress color="inherit" size={20} /> : null}
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
              <FormTextField control={control} name="title" label={t('title')} />
              <Stack spacing={2}>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="space-between" alignItems={{ xs: 'stretch', sm: 'center' }}>
                  <Typography variant="h4">{t('parts')}</Typography>
                  <Button type="button" variant="outlined" startIcon={<AddRoundedIcon />} onClick={() => appendPart({ name: '', price: 0 })}>
                    {t('addPart')}
                  </Button>
                </Stack>
                {partFields.length ? (
                  <Stack spacing={1.5}>
                    {partFields.map((part, index) => (
                      <Box key={part.id} sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 180px auto' }, gap: 1.5, alignItems: 'start' }}>
                        <FormTextField control={control} name={`parts.${index}.name`} label={t('partName')} />
                        <FormTextField control={control} name={`parts.${index}.price`} label={t('price')} type="number" />
                        <IconButton aria-label={`${t('delete')} ${index + 1}`} onClick={() => removePart(index)} sx={{ mt: { sm: 1 } }}>
                          <DeleteRoundedIcon />
                        </IconButton>
                      </Box>
                    ))}
                  </Stack>
                ) : (
                  <Typography color="text.secondary">{t('noOfferParts')}</Typography>
                )}
              </Stack>
            </Stack>
          </Paper>
          <Paper sx={{ p: { xs: 3, md: 4 }, position: { lg: 'sticky' }, top: { lg: 24 } }}>
            <Stack spacing={2}>
              <Typography variant="h4">{t('priceBreakdown')}</Typography>
              <FormTextField control={control} name="laborCost" label={t('laborCost')} type="number" />
              <Divider />
              <Typography fontWeight={700}>{t('partsTotal', { amount: partsCost.toLocaleString('mk-MK') })}</Typography>
              {selectedCustomerId && loyaltyQuery.data ? (
                <Alert severity={loyaltyQuery.data.loyal ? 'success' : 'info'}>
                  {loyaltyQuery.data.loyal
                    ? t('loyalCustomerDiscount', { count: loyaltyQuery.data.completedServices, percent: loyaltyQuery.data.discountPercent })
                    : t('loyalCustomerProgress', { count: loyaltyQuery.data.completedServices, required: loyaltyQuery.data.requiredServices })}
                </Alert>
              ) : null}
              <TextField label={t('subtotal')} value={`${subtotalCost.toLocaleString('mk-MK')} ден.`} InputProps={{ readOnly: true }} />
              <TextField label={t('discount')} value={`-${discountAmount.toLocaleString('mk-MK')} ден. (${discountPercent.toLocaleString('mk-MK')}%)`} InputProps={{ readOnly: true }} />
              <TextField label={t('total')} value={`${totalCost.toLocaleString('mk-MK')} ден.`} InputProps={{ readOnly: true }} />
              <Button type="submit" variant="contained" disabled={formState.isSubmitting || createMutation.isPending}>
                {t('saveAndSend')}
              </Button>
            </Stack>
          </Paper>
        </Box>
      </Stack>
    );
  }

  if (mode === 'detail') {
    if (detailQuery.isLoading) return <LoadingState />;
    const offer = detailQuery.data;
    if (!offer) return <EmptyState />;
    return (
      <Stack spacing={3}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }}>
          <Box>
            <Typography variant="h2">{offer.title}</Typography>
            <Typography color="text.secondary">
              {offer.customerName ?? offer.customerId} - {offerVehicleLabel(offer)}
            </Typography>
          </Box>
          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              disabled={exportMutation.isPending}
              onClick={async () => {
                const response = await exportMutation.mutateAsync(offer.id);
                downloadBlob(response.data, `offer-${offer.id}.pdf`);
                showToast('PDF');
              }}
            >
              PDF
            </Button>
            <Button
              variant="contained"
              disabled={offer.status !== 'DRAFT' || sendMutation.isPending}
              onClick={async () => {
                await sendMutation.mutateAsync(offer.id);
                await queryClient.invalidateQueries({ queryKey: ['offers', id] });
                showToast(t('offerSent'));
              }}
            >
              {t('send')}
            </Button>
          </Stack>
        </Stack>
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: 'minmax(0, 1fr) 360px' }, gap: 3, alignItems: 'start' }}>
          <Stack spacing={3}>
            <Paper sx={{ p: { xs: 3, md: 4 } }}>
              <Typography variant="h3" sx={{ mb: 2 }}>
                {t('customer')} / {t('vehicle')}
              </Typography>
              <Stack spacing={1.5}>
                <DetailRow label={t('customer')} value={offer.customerName ?? offer.customerId} />
                <DetailRow label={t('vehicle')} value={offerVehicleLabel(offer)} />
                <DetailRow label={t('status')} value={offer.status} />
              </Stack>
            </Paper>
            <Paper sx={{ p: { xs: 3, md: 4 } }}>
              <Typography variant="h3" sx={{ mb: 2 }}>
                {t('parts')}
              </Typography>
              {offer.parts.length ? (
                <Table>
                  <TableHead>
                    <TableRow>
                      <TableCell>{t('partName')}</TableCell>
                      <TableCell align="right">{t('price')}</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {offer.parts.map((part) => (
                      <TableRow key={`${part.name}-${part.price}`}>
                        <TableCell>{part.name}</TableCell>
                        <TableCell align="right">{part.price.toLocaleString('mk-MK')} ден.</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              ) : (
                <Typography color="text.secondary">{t('noOfferParts')}</Typography>
              )}
            </Paper>
          </Stack>
          <Paper sx={{ p: { xs: 3, md: 4 }, position: { lg: 'sticky' }, top: { lg: 24 } }}>
            <Typography variant="h3" sx={{ mb: 2 }}>
              {t('priceBreakdown')}
            </Typography>
            <Stack spacing={1.5}>
              <CostRow label={t('partsCost')} value={`${offer.partsCost.toLocaleString('mk-MK')} ден.`} />
              <CostRow label={t('laborCost')} value={`${offer.laborCost.toLocaleString('mk-MK')} ден.`} />
              <Divider />
              <CostRow label={t('subtotal')} value={`${offer.subtotal.toLocaleString('mk-MK')} ден.`} />
              {offer.discountAmount > 0 ? (
                <CostRow label={t('discount')} value={`-${offer.discountAmount.toLocaleString('mk-MK')} ден. (${offer.discountPercent.toLocaleString('mk-MK')}%)`} />
              ) : null}
              <Divider />
              <CostRow label={t('total')} value={`${offer.total.toLocaleString('mk-MK')} ден.`} strong />
            </Stack>
          </Paper>
        </Box>
      </Stack>
    );
  }

  if (listQuery.isLoading) return <LoadingState />;
  const offers = listQuery.data ?? [];

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }}>
        <Typography variant="h2">{t('offers')}</Typography>
        <Button component={RouterLink} to="/offers/new" variant="contained" startIcon={<AddRoundedIcon />}>
          {t('createOffer')}
        </Button>
      </Stack>
      {offers.length ? (
        <Paper sx={{ overflow: 'auto' }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>{t('offer')}</TableCell>
                <TableCell>{t('customer')}</TableCell>
                <TableCell>{t('status')}</TableCell>
                <TableCell align="right">{t('parts')}</TableCell>
                <TableCell align="right">{t('labor')}</TableCell>
                <TableCell align="right">{t('discount')}</TableCell>
                <TableCell align="right">{t('total')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {offers.map((offer) => (
                <TableRow key={offer.id} hover component={RouterLink} to={`/offers/${offer.id}`} sx={{ cursor: 'pointer' }}>
                  <TableCell>{offer.title}</TableCell>
                  <TableCell>{offer.customerName ?? offer.customerId}</TableCell>
                  <TableCell>
                    <Chip label={offer.status} color={offer.status === 'SENT' ? 'secondary' : 'default'} size="small" />
                  </TableCell>
                  <TableCell align="right">{offer.partsCost.toLocaleString('mk-MK')} ден.</TableCell>
                  <TableCell align="right">{offer.laborCost.toLocaleString('mk-MK')} ден.</TableCell>
                  <TableCell align="right">-{offer.discountAmount.toLocaleString('mk-MK')} ден.</TableCell>
                  <TableCell align="right">{offer.total.toLocaleString('mk-MK')} ден.</TableCell>
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

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2, py: 1, borderBottom: 1, borderColor: 'divider' }}>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography fontWeight={700} textAlign="right" sx={{ overflowWrap: 'anywhere' }}>
        {value}
      </Typography>
    </Box>
  );
}

function CostRow({ label, value, strong = false }: { label: string; value: string; strong?: boolean }) {
  return (
    <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2 }}>
      <Typography fontWeight={strong ? 800 : 500}>{label}</Typography>
      <Typography fontWeight={strong ? 800 : 700}>{value}</Typography>
    </Box>
  );
}

function sumParts(parts: OfferPart[]) {
  return parts.reduce((total, part) => total + (Number(part.price) || 0), 0);
}

function customerLabel(customer: Customer) {
  return customer.name;
}

function vehicleLabel(vehicle: Vehicle) {
  return `${vehicle.plate} - ${vehicle.make} ${vehicle.model}`;
}

function offerVehicleLabel(offer: Offer) {
  return [offer.vehiclePlate, offer.vehicleName].filter(Boolean).join(' - ') || offer.vehicleId || '-';
}

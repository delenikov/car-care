import { zodResolver } from '@hookform/resolvers/zod';
import { Box, Button, Chip, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom';
import { z } from 'zod';
import { offersApi } from '../api/modules';
import { FormTextField } from '../components/FormTextField';
import { EmptyState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';

const schema = z.object({
  customerId: z.string().min(1),
  vehicleId: z.string().optional(),
  title: z.string().min(1),
  partsCost: z.coerce.number().min(0),
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
  const { control, handleSubmit, formState } = useForm<OfferForm>({
    resolver: zodResolver(schema) as never,
    defaultValues: { customerId: '', vehicleId: '', title: '', partsCost: 0, laborCost: 0, status: 'DRAFT' }
  });

  if (mode === 'create') {
    const onSubmit = handleSubmit(async (values) => {
      const created = await createMutation.mutateAsync({ ...values, total: values.partsCost + values.laborCost });
      await queryClient.invalidateQueries({ queryKey: ['offers'] });
      showToast(t('saved'));
      navigate(`/offers/${created.id}`);
    });

    return (
      <Stack spacing={3}>
        <Typography variant="h2">{t('createOffer')}</Typography>
        <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 } }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 3 }}>
            <FormTextField control={control} name="customerId" label="Customer ID" />
            <FormTextField control={control} name="vehicleId" label="Vehicle ID" />
            <FormTextField control={control} name="title" label="Title" />
            <FormTextField control={control} name="partsCost" label="Parts cost" type="number" />
            <FormTextField control={control} name="laborCost" label="Labor cost" type="number" />
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
    const offer = detailQuery.data;
    if (!offer) return <EmptyState />;
    return (
      <Stack spacing={3}>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }}>
          <Box>
            <Typography variant="h2">{offer.title}</Typography>
            <Typography color="text.secondary">{offer.total.toLocaleString('mk-MK')} den.</Typography>
          </Box>
          <Stack direction="row" spacing={1}>
            <Button
              variant="outlined"
              disabled={exportMutation.isPending}
              onClick={async () => {
                await exportMutation.mutateAsync(offer.id);
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
                showToast(t('send'));
              }}
            >
              {t('send')}
            </Button>
          </Stack>
        </Stack>
        <Paper sx={{ p: { xs: 3, md: 4 } }}>
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' }, gap: 2 }}>
            <OfferField label="Customer" value={offer.customerId} />
            <OfferField label="Vehicle" value={offer.vehicleId ?? '-'} />
            <OfferField label="Parts cost" value={`${offer.partsCost.toLocaleString('mk-MK')} den.`} />
            <OfferField label="Labor cost" value={`${offer.laborCost.toLocaleString('mk-MK')} den.`} />
            <OfferField label="Status" value={offer.status} />
            <OfferField label="Total" value={`${offer.total.toLocaleString('mk-MK')} den.`} />
          </Box>
        </Paper>
      </Stack>
    );
  }

  if (listQuery.isLoading) return <LoadingState />;
  const offers = listQuery.data ?? [];

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="space-between" alignItems={{ xs: 'flex-start', sm: 'center' }}>
        <Typography variant="h2">{t('offers')}</Typography>
        <Button component={RouterLink} to="/offers/new" variant="contained">
          {t('createOffer')}
        </Button>
      </Stack>
      {offers.length ? (
        <Paper sx={{ overflow: 'auto' }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Offer</TableCell>
                <TableCell>Customer</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Parts</TableCell>
                <TableCell align="right">Labor</TableCell>
                <TableCell align="right">Total</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {offers.map((offer) => (
                <TableRow key={offer.id} hover component={RouterLink} to={`/offers/${offer.id}`} sx={{ cursor: 'pointer' }}>
                  <TableCell>{offer.title}</TableCell>
                  <TableCell>{offer.customerId}</TableCell>
                  <TableCell>
                    <Chip label={offer.status} color={offer.status === 'SENT' ? 'secondary' : 'default'} size="small" />
                  </TableCell>
                  <TableCell align="right">{offer.partsCost.toLocaleString('mk-MK')} den.</TableCell>
                  <TableCell align="right">{offer.laborCost.toLocaleString('mk-MK')} den.</TableCell>
                  <TableCell align="right">{offer.total.toLocaleString('mk-MK')} den.</TableCell>
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

function OfferField({ label, value }: { label: string; value: string }) {
  return (
    <Box sx={{ p: 2, border: 1, borderColor: 'divider', borderRadius: 2 }}>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography fontWeight={700}>{value}</Typography>
    </Box>
  );
}

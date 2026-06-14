import { Box, Button, Paper, Stack, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';
import { appointmentsApi } from '../api/modules';
import { EmptyState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';
import { skopjeDisplayDate, skopjeTime } from '../utils/dateTime';

export function CancelReservationPage() {
  const { t } = useTranslation();
  const { token } = useParams();
  const { showToast } = useToast();
  const queryClient = useQueryClient();
  const infoQuery = useQuery({
    queryKey: ['appointments', 'cancel-info', token],
    queryFn: () => appointmentsApi.cancelInfo(token!),
    enabled: Boolean(token)
  });
  const cancelMutation = useMutation({ mutationFn: () => appointmentsApi.confirmCancel(token!) });

  if (!token) return <EmptyState />;
  if (infoQuery.isLoading) return <LoadingState />;
  if (!infoQuery.data) return <EmptyState />;

  const info = infoQuery.data;

  return (
    <Box sx={{ minHeight: '100vh', bgcolor: 'background.default', p: { xs: 2, md: 4 } }}>
      <Stack spacing={3} sx={{ maxWidth: 720, mx: 'auto' }}>
        <Typography variant="h2">{t('cancelAppointment')}</Typography>
        <Paper sx={{ p: { xs: 3, md: 4 } }}>
          <Stack spacing={2.5}>
            <ReservationField label={t('customer')} value={info.customerName} />
            <ReservationField label={t('vehicle')} value={`${info.vehiclePlate} - ${info.vehicleName}`} />
            <ReservationField label={t('serviceType')} value={info.serviceType} />
            <ReservationField label={t('date')} value={skopjeDisplayDate(info.scheduledAt)} />
            <ReservationField label={t('time')} value={`${skopjeTime(info.scheduledAt)} - ${skopjeTime(info.endsAt)}`} />
            <Typography color={info.cancellable ? 'text.secondary' : 'error'}>{info.message}</Typography>
            <Button
              variant="contained"
              color="error"
              disabled={!info.cancellable || cancelMutation.isPending}
              onClick={async () => {
                await cancelMutation.mutateAsync();
                showToast(t('appointmentCancelled'));
                await queryClient.invalidateQueries({ queryKey: ['appointments'] });
                await infoQuery.refetch();
              }}
            >
              {t('cancelAppointment')}
            </Button>
          </Stack>
        </Paper>
      </Stack>
    </Box>
  );
}

function ReservationField({ label, value }: { label: string; value: string }) {
  return (
    <Box>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography fontWeight={700}>{value}</Typography>
    </Box>
  );
}

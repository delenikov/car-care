import { useQuery } from '@tanstack/react-query';
import { Box, Card, CardContent, Chip, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { dashboardApi } from '../api/modules';
import { LoadingState } from '../components/LoadingState';

const fallbackSummary = {
  customers: 0,
  vehicles: 0,
  appointments: 0,
  serviceRecords: 0,
  offers: 0
};

export function DashboardPage() {
  const { t } = useTranslation();
  const { data, isLoading } = useQuery({ queryKey: ['dashboard-summary'], queryFn: dashboardApi.summary });
  const summary = data ?? fallbackSummary;

  if (isLoading) {
    return <LoadingState />;
  }

  const stats = [
    { label: 'Customers', value: summary.customers },
    { label: 'Vehicles', value: summary.vehicles },
    { label: 'Appointments', value: summary.appointments },
    { label: 'Service records', value: summary.serviceRecords }
  ];

  return (
    <Stack spacing={4}>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1.25fr 0.75fr' }, gap: 3, alignItems: 'stretch' }}>
        <Card sx={{ bgcolor: 'primary.dark', color: 'primary.contrastText', overflow: 'hidden', position: 'relative' }}>
          <Box sx={{ position: 'absolute', inset: 'auto -8% -36% auto', width: { xs: 180, md: 300 }, height: { xs: 180, md: 300 }, borderRadius: '50%', bgcolor: 'secondary.main', opacity: 0.35 }} />
          <CardContent sx={{ p: { xs: 3, md: 5 }, position: 'relative' }}>
            <Chip label="ASMS" color="secondary" sx={{ mb: 3 }} />
            <Typography variant="h2" sx={{ maxWidth: 760 }}>
              {t('dashboard')}
            </Typography>
            <Typography sx={{ mt: 2, maxWidth: 580, opacity: 0.78 }}>
              Track customers, vehicles, appointments, service records, and offers from one workspace.
            </Typography>
          </CardContent>
        </Card>
        <Card>
          <CardContent sx={{ p: { xs: 3, md: 4 } }}>
            <Typography variant="h4" gutterBottom>
              Overview
            </Typography>
            <Stack spacing={2}>
              <Box sx={{ p: 2, border: 1, borderColor: 'divider', borderRadius: 3 }}>
                <Typography fontWeight={700}>{summary.offers}</Typography>
                <Typography color="text.secondary">Offers</Typography>
              </Box>
              <Box sx={{ p: 2, border: 1, borderColor: 'divider', borderRadius: 3 }}>
                <Typography fontWeight={700}>{summary.serviceRecords}</Typography>
                <Typography color="text.secondary">Service records</Typography>
              </Box>
            </Stack>
          </CardContent>
        </Card>
      </Box>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', xl: 'repeat(4, 1fr)' }, gap: 2 }}>
        {stats.map((stat) => (
          <Card key={stat.label}>
            <CardContent>
              <Typography variant="body2" color="text.secondary">
                {stat.label}
              </Typography>
              <Typography variant="h3">{stat.value}</Typography>
            </CardContent>
          </Card>
        ))}
      </Box>
    </Stack>
  );
}

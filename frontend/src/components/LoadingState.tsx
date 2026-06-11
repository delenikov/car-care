import { Box, CircularProgress, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';

export function LoadingState() {
  const { t } = useTranslation();
  return (
    <Box sx={{ display: 'grid', placeItems: 'center', py: 8 }}>
      <Stack alignItems="center" spacing={2}>
        <CircularProgress color="secondary" />
        <Typography color="text.secondary">{t('loading')}</Typography>
      </Stack>
    </Box>
  );
}

export function EmptyState({ label }: { label?: string }) {
  const { t } = useTranslation();
  return (
    <Box sx={{ border: 1, borderColor: 'divider', borderRadius: 3, p: 4, textAlign: 'center', bgcolor: 'background.paper' }}>
      <Typography color="text.secondary">{label ?? t('noData')}</Typography>
    </Box>
  );
}

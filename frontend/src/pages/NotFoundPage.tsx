import { Button, Paper, Stack, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';

export function NotFoundPage() {
  return (
    <Paper sx={{ p: { xs: 3, md: 5 }, maxWidth: 680 }}>
      <Stack spacing={2}>
        <Typography variant="h2">404</Typography>
        <Typography color="text.secondary">Страницата не е пронајдена.</Typography>
        <Button component={RouterLink} to="/customers" variant="contained">
          Назад кон клиенти
        </Button>
      </Stack>
    </Paper>
  );
}

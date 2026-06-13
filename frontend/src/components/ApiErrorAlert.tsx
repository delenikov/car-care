import ErrorOutlineRoundedIcon from '@mui/icons-material/ErrorOutlineRounded';
import { Box } from '@mui/material';

export const apiErrorMessage = (error: unknown, fallback = 'Request failed') => {
  if (typeof error === 'object' && error && 'response' in error) {
    const data = (error as { response?: { data?: { message?: string } } }).response?.data;
    if (data?.message) {
      return data.message;
    }
  }
  return fallback;
};

export function ApiErrorAlert({ message }: { message: string }) {
  if (!message) {
    return null;
  }

  return (
    <Box
      role="alert"
      sx={{
        px: 2,
        py: 1.5,
        borderRadius: 2,
        bgcolor: '#fee2e2',
        color: '#7f1d1d',
        border: '1px solid #fecaca',
        display: 'flex',
        alignItems: 'center',
        gap: 1,
        fontWeight: 700
      }}
    >
      <ErrorOutlineRoundedIcon fontSize="small" />
      <Box component="span">{message}</Box>
    </Box>
  );
}

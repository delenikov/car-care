import ErrorOutlineRoundedIcon from '@mui/icons-material/ErrorOutlineRounded';
import { Box, Stack, Typography } from '@mui/material';
import { apiErrorMessage, apiFieldErrors, normalizeApiError } from '../api/http';

export { apiErrorMessage, apiFieldErrors, normalizeApiError };

export function ApiErrorAlert({ message, fieldErrors = {} }: { message: string; fieldErrors?: Record<string, string> }) {
  if (!message) {
    return null;
  }

  const fieldErrorValues = Object.values(fieldErrors).filter(Boolean);

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
        alignItems: 'flex-start',
        gap: 1,
        fontWeight: 700
      }}
    >
      <ErrorOutlineRoundedIcon fontSize="small" sx={{ mt: 0.25 }} />
      <Stack spacing={0.5}>
        <Box component="span">{message}</Box>
        {fieldErrorValues.length ? (
          <Box component="ul" sx={{ m: 0, pl: 2.5, fontWeight: 500 }}>
            {fieldErrorValues.map((fieldError) => (
              <Typography key={fieldError} component="li" variant="body2">
                {fieldError}
              </Typography>
            ))}
          </Box>
        ) : null}
      </Stack>
    </Box>
  );
}

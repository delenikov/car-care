import { createContext, useContext, useMemo, useState, type ReactNode } from 'react';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import { Box, IconButton, Snackbar } from '@mui/material';

type ToastSeverity = 'success' | 'info' | 'warning' | 'error';

interface ToastState {
  message: string;
  severity: ToastSeverity;
}

interface ToastContextValue {
  showToast: (message: string, severity?: ToastSeverity) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

const toastColors: Record<ToastSeverity, string> = {
  success: '#15803d',
  info: '#2563eb',
  warning: '#b7791f',
  error: '#b42318'
};

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<ToastState | null>(null);
  const value = useMemo(
    () => ({
      showToast: (message: string, severity: ToastSeverity = 'success') => setToast({ message, severity })
    }),
    []
  );

  return (
    <ToastContext.Provider value={value}>
      {children}
      <Snackbar
        open={Boolean(toast)}
        autoHideDuration={6000}
        onClose={() => setToast(null)}
        anchorOrigin={{ vertical: 'top', horizontal: 'center' }}
        sx={{ zIndex: (theme) => theme.zIndex.tooltip + 1, mt: { xs: 1, sm: 2 } }}
      >
        <Box
          role="alert"
          data-testid="app-toast"
          sx={{
            minWidth: { xs: 'calc(100vw - 32px)', sm: 360 },
            maxWidth: 'calc(100vw - 32px)',
            px: 2,
            py: 1.25,
            borderRadius: 2,
            backgroundColor: toastColors[toast?.severity ?? 'success'],
            backgroundImage: 'none',
            border: '1px solid rgba(255, 255, 255, 0.22)',
            color: '#ffffff',
            display: 'flex',
            alignItems: 'center',
            gap: 1.5,
            fontWeight: 700,
            boxShadow: 8,
          }}
        >
          <Box component="span" sx={{ flex: 1 }}>
            {toast?.message}
          </Box>
          <IconButton aria-label="Close notification" size="small" onClick={() => setToast(null)} sx={{ color: '#ffffff' }}>
            <CloseRoundedIcon fontSize="small" />
          </IconButton>
        </Box>
      </Snackbar>
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used inside ToastProvider');
  }
  return context;
}

import { Component, type ErrorInfo, type ReactNode } from 'react';
import { Box, Button, Paper, Typography } from '@mui/material';

interface State {
  hasError: boolean;
}

export class ErrorBoundary extends Component<{ children: ReactNode }, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error(error, info);
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }

    return (
      <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', p: { xs: 2, md: 4 } }}>
        <Paper sx={{ p: { xs: 3, md: 5 }, maxWidth: 560 }}>
          <Typography variant="h4" gutterBottom>
            Системот наиде на проблем
          </Typography>
          <Typography color="text.secondary" sx={{ mb: 3 }}>
            Освежете ја страницата или обидете се повторно по кратко време.
          </Typography>
          <Button variant="contained" onClick={() => window.location.reload()}>
            Освежи
          </Button>
        </Paper>
      </Box>
    );
  }
}

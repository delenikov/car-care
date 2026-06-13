import { Box, Button, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { documentsApi } from '../api/modules';
import { EmptyState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';
import type { DocumentRecord } from '../types';

export function DocumentsPage() {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const { data = [], isLoading } = useQuery({ queryKey: ['documents'], queryFn: documentsApi.list });
  const [selected, setSelected] = useState<DocumentRecord | null>(null);
  const sendMutation = useMutation({ mutationFn: documentsApi.send });
  const exportMutation = useMutation({ mutationFn: documentsApi.exportPdf });

  if (isLoading) return <LoadingState />;

  return (
    <Stack spacing={3}>
      <Typography variant="h2">{t('documents')}</Typography>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 0.8fr' }, gap: 3 }}>
        {data.length ? (
          <Paper sx={{ overflow: 'auto' }}>
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell>Document</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Service record</TableCell>
                  <TableCell align="right">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {data.map((document) => (
                  <TableRow key={document.id} hover>
                    <TableCell>{document.title}</TableCell>
                    <TableCell>{document.type}</TableCell>
                    <TableCell>{document.serviceRecordId ?? '-'}</TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={1} justifyContent="flex-end">
                        <Button onClick={() => setSelected(document)}>View</Button>
                        <Button
                          onClick={async () => {
                            await exportMutation.mutateAsync(document.id);
                            showToast('PDF');
                          }}
                        >
                          PDF
                        </Button>
                        <Button
                          variant="contained"
                          onClick={async () => {
                            await sendMutation.mutateAsync(document.id);
                            showToast(t('send'));
                          }}
                        >
                          {t('send')}
                        </Button>
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
        ) : (
          <EmptyState />
        )}
        <Paper sx={{ minHeight: 360, p: { xs: 3, md: 4 }, display: 'grid', placeItems: 'center', textAlign: 'center' }}>
          <Stack spacing={1} alignItems="center">
            <Typography variant="h4">{selected?.title ?? 'PDF'}</Typography>
            <Typography color="text.secondary">{selected ? selected.storageKey : t('pdfPreview')}</Typography>
          </Stack>
        </Paper>
      </Box>
    </Stack>
  );
}

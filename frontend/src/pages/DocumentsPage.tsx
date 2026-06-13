import { Box, Button, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { documentsApi } from '../api/modules';
import { EmptyState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';
import type { DocumentRecord } from '../types';
import { downloadBlob } from '../utils/download';

export function DocumentsPage() {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const { data = [], isLoading } = useQuery({ queryKey: ['documents'], queryFn: documentsApi.list });
  const [selected, setSelected] = useState<DocumentRecord | null>(null);
  const [previewUrl, setPreviewUrl] = useState('');
  const sendMutation = useMutation({ mutationFn: documentsApi.send });
  const exportMutation = useMutation({ mutationFn: documentsApi.exportPdf });
  const previewMutation = useMutation({ mutationFn: documentsApi.exportPdf });

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

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
                  <TableCell>{t('document')}</TableCell>
                  <TableCell>{t('type')}</TableCell>
                  <TableCell>{t('serviceRecord')}</TableCell>
                  <TableCell align="right">{t('actions')}</TableCell>
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
                        <Button
                          disabled={previewMutation.isPending}
                          onClick={async () => {
                            const response = await previewMutation.mutateAsync(document.id);
                            const nextUrl = URL.createObjectURL(response.data);
                            setPreviewUrl((currentUrl) => {
                              if (currentUrl) {
                                URL.revokeObjectURL(currentUrl);
                              }
                              return nextUrl;
                            });
                            setSelected(document);
                          }}
                        >
                          {t('view')}
                        </Button>
                        <Button
                          onClick={async () => {
                            const response = await exportMutation.mutateAsync(document.id);
                            downloadBlob(response.data, document.title || `document-${document.id}.pdf`);
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
        <Paper sx={{ minHeight: 520, p: { xs: 2, md: 3 }, display: 'grid', gridTemplateRows: 'auto 1fr', gap: 2 }}>
          <Typography variant="h4">{selected?.title ?? 'PDF'}</Typography>
          {previewUrl ? (
            <Box
              component="iframe"
              title={selected?.title ?? 'PDF'}
              src={previewUrl}
              sx={{ width: '100%', height: { xs: 420, lg: 620 }, border: 0, borderRadius: 1, bgcolor: 'background.default' }}
            />
          ) : (
            <Stack spacing={1} alignItems="center" justifyContent="center" sx={{ textAlign: 'center' }}>
              <Typography color="text.secondary">{t('pdfPreview')}</Typography>
            </Stack>
          )}
        </Paper>
      </Box>
    </Stack>
  );
}

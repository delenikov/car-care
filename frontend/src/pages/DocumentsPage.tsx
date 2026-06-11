import { Box, Button, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { documentsApi } from '../api/modules';
import { EmptyState, LoadingState } from '../components/LoadingState';
import type { DocumentRecord } from '../types';

export function DocumentsPage() {
  const { t } = useTranslation();
  const { data = [], isLoading } = useQuery({ queryKey: ['documents'], queryFn: documentsApi.list });
  const [selected, setSelected] = useState<DocumentRecord | null>(null);

  if (isLoading) return <LoadingState />;

  return (
    <Stack spacing={3}>
      <Typography variant="h2">{t('documents')}</Typography>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 0.8fr' }, gap: 3 }}>
        {data.length ? (
          <Paper sx={{ overflow: 'auto' }}>
            <Table>
              <TableHead><TableRow><TableCell>Документ</TableCell><TableCell>Тип</TableCell><TableCell>Датум</TableCell><TableCell align="right">Преглед</TableCell></TableRow></TableHead>
              <TableBody>
                {data.map((document) => (
                  <TableRow key={document.id} hover>
                    <TableCell>{document.title}</TableCell>
                    <TableCell>{document.type}</TableCell>
                    <TableCell>{new Date(document.uploadedAt).toLocaleDateString('mk-MK')}</TableCell>
                    <TableCell align="right"><Button onClick={() => setSelected(document)}>PDF</Button></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Paper>
        ) : <EmptyState />}
        <Paper sx={{ minHeight: 360, p: { xs: 3, md: 4 }, display: 'grid', placeItems: 'center', textAlign: 'center' }}>
          <Stack spacing={1} alignItems="center">
            <Typography variant="h4">{selected?.title ?? 'PDF'}</Typography>
            <Typography color="text.secondary">{t('pdfPreview')}</Typography>
          </Stack>
        </Paper>
      </Box>
    </Stack>
  );
}

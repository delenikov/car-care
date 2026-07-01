import { Box, MenuItem, Pagination, Select, Stack, Typography } from '@mui/material';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';

const DEFAULT_PAGE_SIZE = 10;
const DEFAULT_PAGE_SIZE_OPTIONS = [10, 25, 50];

export function useListPagination<T>(items: readonly T[], initialPageSize = DEFAULT_PAGE_SIZE) {
  const [page, setPage] = useState(1);
  const [pageSize, setPageSizeState] = useState(initialPageSize);
  const totalItems = items.length;
  const pageCount = Math.max(1, Math.ceil(totalItems / pageSize));
  const currentPage = Math.min(page, pageCount);

  useEffect(() => {
    setPage((current) => Math.min(current, pageCount));
  }, [pageCount]);

  const pageItems = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    return items.slice(start, start + pageSize);
  }, [currentPage, items, pageSize]);

  const setPageSize = (nextPageSize: number) => {
    setPageSizeState(nextPageSize);
    setPage(1);
  };

  return {
    page: currentPage,
    pageCount,
    pageItems,
    pageSize,
    setPage,
    setPageSize,
    totalItems
  };
}

export function ListPagination({
  page,
  pageCount,
  pageSize,
  pageSizeOptions = DEFAULT_PAGE_SIZE_OPTIONS,
  totalItems,
  onPageChange,
  onPageSizeChange
}: {
  page: number;
  pageCount: number;
  pageSize: number;
  pageSizeOptions?: number[];
  totalItems: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (pageSize: number) => void;
}) {
  const { t } = useTranslation();
  const availablePageSizes = Array.from(new Set([pageSize, ...pageSizeOptions])).sort((first, second) => first - second);

  if (totalItems <= Math.min(...availablePageSizes)) {
    return null;
  }

  const from = totalItems ? (page - 1) * pageSize + 1 : 0;
  const to = Math.min(totalItems, page * pageSize);

  return (
    <Stack
      direction={{ xs: 'column', md: 'row' }}
      spacing={2}
      alignItems={{ xs: 'stretch', md: 'center' }}
      justifyContent="space-between"
      sx={{ px: 2, py: 1.5, borderTop: 1, borderColor: 'divider' }}
    >
      <Typography variant="body2" color="text.secondary">
        {t('paginationRange', { from, to, total: totalItems })}
      </Typography>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems={{ xs: 'stretch', sm: 'center' }}>
        <Stack direction="row" spacing={1} alignItems="center" justifyContent={{ xs: 'space-between', sm: 'flex-start' }}>
          <Typography variant="body2" color="text.secondary">
            {t('rowsPerPage')}
          </Typography>
          <Select
            size="small"
            value={String(pageSize)}
            onChange={(event) => onPageSizeChange(Number(event.target.value))}
            sx={{ minWidth: 88 }}
          >
            {availablePageSizes.map((option) => (
              <MenuItem key={option} value={String(option)}>
                {option}
              </MenuItem>
            ))}
          </Select>
        </Stack>
        <Box sx={{ display: 'flex', justifyContent: { xs: 'center', sm: 'flex-end' } }}>
          <Pagination
            count={pageCount}
            page={page}
            onChange={(_, value) => onPageChange(value)}
            color="primary"
            size="small"
            showFirstButton
            showLastButton
          />
        </Box>
      </Stack>
    </Stack>
  );
}

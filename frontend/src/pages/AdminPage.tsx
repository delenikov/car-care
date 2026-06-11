import { zodResolver } from '@hookform/resolvers/zod';
import { Box, Button, Chip, Paper, Stack, Tab, Tabs, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { adminUsersApi, loyaltyRulesApi } from '../api/modules';
import { FormTextField } from '../components/FormTextField';
import { EmptyState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';

const employeeSchema = z.object({
  name: z.string().min(1),
  email: z.string().email(),
  role: z.enum(['ADMIN', 'MANAGER', 'EMPLOYEE'])
});

const loyaltySchema = z.object({
  name: z.string().min(1),
  pointsPerDenar: z.coerce.number().min(0),
  active: z.coerce.boolean()
});

type EmployeeForm = z.infer<typeof employeeSchema>;
type LoyaltyForm = z.infer<typeof loyaltySchema>;

export function AdminPage() {
  const { t } = useTranslation();
  const [tab, setTab] = useState(0);

  return (
    <Stack spacing={3}>
      <Typography variant="h2">{t('admin')}</Typography>
      <Paper sx={{ px: 2 }}>
        <Tabs value={tab} onChange={(_, value: number) => setTab(value)} variant="scrollable" scrollButtons="auto">
          <Tab label={t('employees')} />
          <Tab label={t('loyalty')} />
        </Tabs>
      </Paper>
      {tab === 0 ? <EmployeePanel /> : <LoyaltyPanel />}
    </Stack>
  );
}

function EmployeePanel() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const usersQuery = useQuery({ queryKey: ['admin-users'], queryFn: adminUsersApi.list });
  const createMutation = useMutation({ mutationFn: adminUsersApi.create });
  const { control, handleSubmit, reset, formState } = useForm<EmployeeForm>({
    resolver: zodResolver(employeeSchema),
    defaultValues: { name: '', email: '', role: 'EMPLOYEE' }
  });

  const onSubmit = handleSubmit(async (values) => {
    await createMutation.mutateAsync(values);
    await queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    reset();
    showToast(t('saved'));
  });

  if (usersQuery.isLoading) return <LoadingState />;

  return (
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', xl: '1.2fr 0.8fr' }, gap: 3 }}>
      <Paper sx={{ overflow: 'auto' }}>
        {(usersQuery.data ?? []).length ? (
          <Table>
            <TableHead><TableRow><TableCell>Име</TableCell><TableCell>Е-пошта</TableCell><TableCell>Улога</TableCell></TableRow></TableHead>
            <TableBody>{(usersQuery.data ?? []).map((user) => <TableRow key={user.id}><TableCell>{user.fullName}</TableCell><TableCell>{user.email}</TableCell><TableCell><Chip label={user.roles?.[0]?.replace('ROLE_', '') ?? 'EMPLOYEE'} size="small" /></TableCell></TableRow>)}</TableBody>
          </Table>
        ) : <EmptyState />}
      </Paper>
      <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 }, alignSelf: 'start' }}>
        <Stack spacing={2.5}>
          <Typography variant="h4">Нов вработен</Typography>
          <FormTextField control={control} name="name" label="Име" />
          <FormTextField control={control} name="email" label="Е-пошта" />
          <FormTextField control={control} name="role" label="Улога" helperText="ADMIN, MANAGER или EMPLOYEE" />
          <Button type="submit" variant="contained" disabled={formState.isSubmitting}>{t('save')}</Button>
        </Stack>
      </Paper>
    </Box>
  );
}

function LoyaltyPanel() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const rulesQuery = useQuery({ queryKey: ['loyalty-rules'], queryFn: loyaltyRulesApi.list });
  const createMutation = useMutation({ mutationFn: loyaltyRulesApi.create });
  const { control, handleSubmit, reset, formState } = useForm<LoyaltyForm>({
    resolver: zodResolver(loyaltySchema),
    defaultValues: { name: '', pointsPerDenar: 0.01, active: true }
  });

  const onSubmit = handleSubmit(async (values) => {
    await createMutation.mutateAsync(values);
    await queryClient.invalidateQueries({ queryKey: ['loyalty-rules'] });
    reset();
    showToast(t('saved'));
  });

  if (rulesQuery.isLoading) return <LoadingState />;

  return (
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', xl: '1.2fr 0.8fr' }, gap: 3 }}>
      <Paper sx={{ overflow: 'auto' }}>
        {(rulesQuery.data ?? []).length ? (
          <Table>
            <TableHead><TableRow><TableCell>Правило</TableCell><TableCell>Поени/денар</TableCell><TableCell>Статус</TableCell></TableRow></TableHead>
            <TableBody>{(rulesQuery.data ?? []).map((rule) => <TableRow key={rule.id}><TableCell>{rule.name}</TableCell><TableCell>{rule.pointsPerDenar}</TableCell><TableCell><Chip label={rule.active ? 'Активно' : 'Неактивно'} color={rule.active ? 'success' : 'default'} size="small" /></TableCell></TableRow>)}</TableBody>
          </Table>
        ) : <EmptyState />}
      </Paper>
      <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 }, alignSelf: 'start' }}>
        <Stack spacing={2.5}>
          <Typography variant="h4">Лојалност правило</Typography>
          <FormTextField control={control} name="name" label="Име" />
          <FormTextField control={control} name="pointsPerDenar" label="Поени по денар" type="number" />
          <FormTextField control={control} name="active" label="Активно" helperText="true или false" />
          <Button type="submit" variant="contained" disabled={formState.isSubmitting}>{t('save')}</Button>
        </Stack>
      </Paper>
    </Box>
  );
}

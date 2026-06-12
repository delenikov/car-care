import { zodResolver } from '@hookform/resolvers/zod';
import DeleteRoundedIcon from '@mui/icons-material/DeleteRounded';
import EditRoundedIcon from '@mui/icons-material/EditRounded';
import { Box, Button, Chip, IconButton, Paper, Stack, Tab, Tabs, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { adminUsersApi, loyaltyRulesApi } from '../api/modules';
import { FormTextField } from '../components/FormTextField';
import { EmptyState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';
import type { Role, User } from '../types';

const employeeSchema = z.object({
  fullName: z.string().min(1),
  email: z.string().email(),
  password: z.string().min(8).optional().or(z.literal('')),
  enabled: z.coerce.boolean(),
  role: z.enum(['ADMIN', 'MANAGER', 'EMPLOYEE'])
});

const loyaltySchema = z.object({
  name: z.string().min(1),
  pointsPerDenar: z.coerce.number().min(0),
  active: z.coerce.boolean()
});

type EmployeeForm = z.output<typeof employeeSchema>;
type LoyaltyForm = z.output<typeof loyaltySchema>;

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
  const updateMutation = useMutation({ mutationFn: ({ id, values }: { id: string; values: EmployeeForm }) => adminUsersApi.update(id, toEmployeePayload(values)) });
  const deleteMutation = useMutation({ mutationFn: adminUsersApi.remove });
  const [editingId, setEditingId] = useState<string | null>(null);

  const { control, handleSubmit, reset, formState } = useForm<EmployeeForm>({
    resolver: zodResolver(employeeSchema) as never,
    defaultValues: { fullName: '', email: '', password: '', enabled: true, role: 'EMPLOYEE' }
  });

  const onSubmit = handleSubmit(async (values) => {
    if (editingId) {
      await updateMutation.mutateAsync({ id: editingId, values });
    } else {
      await createMutation.mutateAsync(toEmployeePayload(values));
    }
    await queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    setEditingId(null);
    reset(emptyEmployeeForm());
    showToast(t('saved'));
  });

  const startEdit = (user: User) => {
    setEditingId(user.id);
    reset({
      fullName: user.fullName,
      email: user.email,
      password: '',
      enabled: user.enabled,
      role: roleToFormValue(user.roles?.[0])
    });
  };

  const deleteUser = async (user: User) => {
    await deleteMutation.mutateAsync(user.id);
    await queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    showToast(t('updated'));
  };

  if (usersQuery.isLoading) return <LoadingState />;

  return (
    <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', xl: '1.2fr 0.8fr' }, gap: 3 }}>
      <Paper sx={{ overflow: 'auto' }}>
        {(usersQuery.data ?? []).length ? (
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Name</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Role</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(usersQuery.data ?? []).map((user) => (
                <TableRow key={user.id}>
                  <TableCell>{user.fullName}</TableCell>
                  <TableCell>{user.email}</TableCell>
                  <TableCell><Chip label={roleToFormValue(user.roles?.[0])} size="small" /></TableCell>
                  <TableCell><Chip label={user.enabled ? 'ACTIVE' : 'DISABLED'} color={user.enabled ? 'success' : 'default'} size="small" /></TableCell>
                  <TableCell align="right">
                    <IconButton aria-label={`edit ${user.email}`} onClick={() => startEdit(user)}>
                      <EditRoundedIcon />
                    </IconButton>
                    <IconButton aria-label={`delete ${user.email}`} disabled={deleteMutation.isPending} onClick={() => deleteUser(user)}>
                      <DeleteRoundedIcon />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        ) : <EmptyState />}
      </Paper>
      <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 }, alignSelf: 'start' }}>
        <Stack spacing={2.5}>
          <Typography variant="h4">{editingId ? 'Edit employee' : 'New employee'}</Typography>
          <FormTextField control={control} name="fullName" label="Name" />
          <FormTextField control={control} name="email" label="Email" />
          <FormTextField control={control} name="password" label="Password" type="password" helperText={editingId ? 'Fill only to set a new password' : undefined} />
          <FormTextField control={control} name="enabled" label="Active" helperText="true or false" />
          <FormTextField control={control} name="role" label="Role" helperText="ADMIN, MANAGER or EMPLOYEE" />
          <Button type="submit" variant="contained" disabled={formState.isSubmitting}>{t('save')}</Button>
          {editingId ? (
            <Button type="button" variant="outlined" onClick={() => { setEditingId(null); reset(emptyEmployeeForm()); }}>
              {t('cancel')}
            </Button>
          ) : null}
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
    resolver: zodResolver(loyaltySchema) as never,
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
            <TableHead><TableRow><TableCell>Rule</TableCell><TableCell>Points/denar</TableCell><TableCell>Status</TableCell></TableRow></TableHead>
            <TableBody>{(rulesQuery.data ?? []).map((rule) => <TableRow key={rule.id}><TableCell>{rule.name}</TableCell><TableCell>{rule.pointsPerDenar}</TableCell><TableCell><Chip label={rule.active ? 'ACTIVE' : 'INACTIVE'} color={rule.active ? 'success' : 'default'} size="small" /></TableCell></TableRow>)}</TableBody>
          </Table>
        ) : <EmptyState />}
      </Paper>
      <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 }, alignSelf: 'start' }}>
        <Stack spacing={2.5}>
          <Typography variant="h4">Loyalty rule</Typography>
          <FormTextField control={control} name="name" label="Name" />
          <FormTextField control={control} name="pointsPerDenar" label="Points per denar" type="number" />
          <FormTextField control={control} name="active" label="Active" helperText="true or false" />
          <Button type="submit" variant="contained" disabled={formState.isSubmitting}>{t('save')}</Button>
        </Stack>
      </Paper>
    </Box>
  );
}

function emptyEmployeeForm(): EmployeeForm {
  return { fullName: '', email: '', password: '', enabled: true, role: 'EMPLOYEE' };
}

function roleToFormValue(role?: Role): EmployeeForm['role'] {
  if (role === 'ROLE_ADMIN') return 'ADMIN';
  if (role === 'ROLE_MANAGER') return 'MANAGER';
  return 'EMPLOYEE';
}

function toEmployeePayload(values: EmployeeForm) {
  return {
    fullName: values.fullName,
    email: values.email,
    password: values.password || undefined,
    enabled: values.enabled,
    roles: [`ROLE_${values.role}` as Role]
  };
}

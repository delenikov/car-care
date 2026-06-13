import { zodResolver } from '@hookform/resolvers/zod';
import AdminPanelSettingsRoundedIcon from '@mui/icons-material/AdminPanelSettingsRounded';
import DeleteRoundedIcon from '@mui/icons-material/DeleteRounded';
import EditRoundedIcon from '@mui/icons-material/EditRounded';
import EngineeringRoundedIcon from '@mui/icons-material/EngineeringRounded';
import { Box, Button, Chip, IconButton, Paper, Stack, Tab, Tabs, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { adminUsersApi, loyaltyRulesApi } from '../api/modules';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { FormTextField } from '../components/FormTextField';
import { EmptyState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';
import type { Role, User } from '../types';

const employeeSchema = z.object({
  fullName: z.string().min(1),
  email: z.string().email(),
  password: z.string().min(8).optional().or(z.literal('')),
  enabled: z.enum(['true', 'false']),
  role: z.enum(['ADMIN', 'EMPLOYEE'])
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
  const [formOpen, setFormOpen] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<User | null>(null);

  const { control, handleSubmit, reset, formState } = useForm<EmployeeForm>({
    resolver: zodResolver(employeeSchema) as never,
    defaultValues: emptyEmployeeForm()
  });

  const onSubmit = handleSubmit(async (values) => {
    if (editingId) {
      await updateMutation.mutateAsync({ id: editingId, values });
    } else {
      await createMutation.mutateAsync(toEmployeePayload(values));
    }
    await queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    setEditingId(null);
    setFormOpen(false);
    reset(emptyEmployeeForm());
    showToast(t('saved'));
  });

  const startCreate = () => {
    setEditingId(null);
    reset(emptyEmployeeForm());
    setFormOpen(true);
  };

  const startEdit = (user: User) => {
    setEditingId(user.id);
    reset({
      fullName: user.fullName,
      email: user.email,
      password: '',
      enabled: user.enabled ? 'true' : 'false',
      role: roleToFormValue(user.roles?.[0])
    });
    setFormOpen(true);
  };

  const deleteUser = async (user: User) => {
    await deleteMutation.mutateAsync(user.id);
    await queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    setDeleteTarget(null);
    showToast(t('updated'));
  };

  if (usersQuery.isLoading) return <LoadingState />;

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="flex-end">
        <Button variant="contained" onClick={startCreate}>
          {t('newEmployee')}
        </Button>
      </Stack>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', xl: formOpen ? '1.2fr 0.8fr' : '1fr' }, gap: 3 }}>
        <Paper sx={{ overflow: 'auto' }}>
        {(usersQuery.data ?? []).length ? (
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>{t('name')}</TableCell>
                <TableCell>{t('email')}</TableCell>
                <TableCell>{t('role')}</TableCell>
                <TableCell>{t('status')}</TableCell>
                <TableCell align="right">{t('actions')}</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {(usersQuery.data ?? []).map((user) => (
                <TableRow key={user.id}>
                  <TableCell>{user.fullName}</TableCell>
                  <TableCell>{user.email}</TableCell>
                  <TableCell><RoleChip role={roleToFormValue(user.roles?.[0])} /></TableCell>
                  <TableCell><Chip label={user.enabled ? 'ACTIVE' : 'DISABLED'} color={user.enabled ? 'success' : 'default'} size="small" /></TableCell>
                  <TableCell align="right">
                    <IconButton aria-label={`edit ${user.email}`} onClick={() => startEdit(user)}>
                      <EditRoundedIcon />
                    </IconButton>
                    <IconButton aria-label={`delete ${user.email}`} disabled={deleteMutation.isPending} onClick={() => setDeleteTarget(user)}>
                      <DeleteRoundedIcon />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        ) : <EmptyState />}
        </Paper>
        {formOpen ? (
          <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 }, alignSelf: 'start' }}>
            <Stack spacing={2.5}>
              <Typography variant="h4">{editingId ? t('editEmployee') : t('newEmployee')}</Typography>
              <FormTextField control={control} name="fullName" label={t('name')} />
              <FormTextField control={control} name="email" label={t('email')} />
              <FormTextField control={control} name="password" label={t('password')} type="password" helperText={editingId ? t('newPasswordHint') : undefined} />
              <FormTextField control={control} name="enabled" label={t('active')} select SelectProps={{ native: true }}>
                <option value="true">{t('active')}</option>
                <option value="false">{t('inactive')}</option>
              </FormTextField>
              <FormTextField control={control} name="role" label={t('role')} select SelectProps={{ native: true }}>
                <option value="EMPLOYEE">{t('employee')}</option>
                <option value="ADMIN">{t('admin')}</option>
              </FormTextField>
              <Button type="submit" variant="contained" disabled={formState.isSubmitting}>{t('save')}</Button>
              <Button type="button" variant="outlined" onClick={() => { setEditingId(null); setFormOpen(false); reset(emptyEmployeeForm()); }}>
              {t('cancel')}
            </Button>
            </Stack>
          </Paper>
        ) : null}
      </Box>
      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t('deleteEmployee')}
        description={t('deleteEmployeeConfirm', { name: deleteTarget?.fullName ?? t('employee') })}
        confirmLabel={t('delete')}
        cancelLabel={t('cancel')}
        confirming={deleteMutation.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget ? deleteUser(deleteTarget) : undefined}
      />
    </Stack>
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
            <TableHead><TableRow><TableCell>{t('rule')}</TableCell><TableCell>{t('pointsPerDenar')}</TableCell><TableCell>{t('status')}</TableCell></TableRow></TableHead>
            <TableBody>{(rulesQuery.data ?? []).map((rule) => <TableRow key={rule.id}><TableCell>{rule.name}</TableCell><TableCell>{rule.pointsPerDenar}</TableCell><TableCell><Chip label={rule.active ? 'ACTIVE' : 'INACTIVE'} color={rule.active ? 'success' : 'default'} size="small" /></TableCell></TableRow>)}</TableBody>
          </Table>
        ) : <EmptyState />}
      </Paper>
      <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 }, alignSelf: 'start' }}>
        <Stack spacing={2.5}>
          <Typography variant="h4">{t('loyaltyRule')}</Typography>
          <FormTextField control={control} name="name" label={t('name')} />
          <FormTextField control={control} name="pointsPerDenar" label={t('pointsPerDenar')} type="number" />
          <FormTextField control={control} name="active" label={t('active')} helperText={`${t('true')} / ${t('false')}`} />
          <Button type="submit" variant="contained" disabled={formState.isSubmitting}>{t('save')}</Button>
        </Stack>
      </Paper>
    </Box>
  );
}

function emptyEmployeeForm(): EmployeeForm {
  return { fullName: '', email: '', password: '', enabled: 'true', role: 'EMPLOYEE' };
}

function roleToFormValue(role?: Role): EmployeeForm['role'] {
  if (role === 'ROLE_ADMIN') return 'ADMIN';
  return 'EMPLOYEE';
}

function RoleChip({ role }: { role: EmployeeForm['role'] }) {
  if (role === 'ADMIN') {
    return <Chip icon={<AdminPanelSettingsRoundedIcon />} label="ADMIN" color="primary" size="small" />;
  }
  return <Chip icon={<EngineeringRoundedIcon />} label="EMPLOYEE" color="secondary" size="small" />;
}

function toEmployeePayload(values: EmployeeForm) {
  return {
    fullName: values.fullName,
    email: values.email,
    password: values.password || undefined,
    enabled: values.enabled === 'true',
    roles: [`ROLE_${values.role}` as Role]
  };
}

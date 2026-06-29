import { zodResolver } from '@hookform/resolvers/zod';
import AdminPanelSettingsRoundedIcon from '@mui/icons-material/AdminPanelSettingsRounded';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import DeleteRoundedIcon from '@mui/icons-material/DeleteRounded';
import EditRoundedIcon from '@mui/icons-material/EditRounded';
import EngineeringRoundedIcon from '@mui/icons-material/EngineeringRounded';
import { Box, Button, Chip, IconButton, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { adminUsersApi } from '../api/modules';
import { ApiErrorAlert, apiErrorMessage } from '../components/ApiErrorAlert';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { FormTextField } from '../components/FormTextField';
import { EmptyState, ErrorState, LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';
import type { Role, User } from '../types';
import { applyApiFieldErrors } from '../utils/apiFormErrors';

const employeeSchema = z.object({
  fullName: z.string().min(1),
  email: z.string().email(),
  password: z.string().min(8).optional().or(z.literal('')),
  enabled: z.enum(['true', 'false']),
  role: z.enum(['ADMIN', 'EMPLOYEE'])
});

type EmployeeForm = z.output<typeof employeeSchema>;

export function AdminPage() {
  const { t } = useTranslation();

  return (
    <Stack spacing={3}>
      <Typography variant="h2">{t('admin')}</Typography>
      <EmployeePanel />
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
  const [errorMessage, setErrorMessage] = useState('');

  const { control, handleSubmit, reset, setError, formState } = useForm<EmployeeForm>({
    resolver: zodResolver(employeeSchema) as never,
    defaultValues: emptyEmployeeForm()
  });

  const onSubmit = handleSubmit(async (values) => {
    setErrorMessage('');
    try {
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
    } catch (error) {
      const fieldErrors = applyApiFieldErrors(error, setError);
      setErrorMessage(apiErrorMessage(error, t('saveFailed')));
      if (!Object.keys(fieldErrors).length) {
        showToast(apiErrorMessage(error, t('saveFailed')), 'error');
      }
    }
  });

  const startCreate = () => {
    setErrorMessage('');
    setEditingId(null);
    reset(emptyEmployeeForm());
    setFormOpen(true);
  };

  const startEdit = (user: User) => {
    setErrorMessage('');
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
    try {
      await deleteMutation.mutateAsync(user.id);
      await queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      setDeleteTarget(null);
      showToast(t('updated'));
    } catch (error) {
      showToast(apiErrorMessage(error, t('deleteFailed')), 'error');
    }
  };

  if (usersQuery.isLoading) return <LoadingState />;
  if (usersQuery.isError) return <ErrorState error={usersQuery.error} />;

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} justifyContent="flex-end">
        <Button variant="contained" startIcon={<AddRoundedIcon />} onClick={startCreate}>
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
              <ApiErrorAlert message={errorMessage} />
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

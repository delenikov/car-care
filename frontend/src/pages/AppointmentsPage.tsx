import { useMemo, useState, type ComponentProps } from 'react';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import { zodResolver } from '@hookform/resolvers/zod';
import { Accordion, AccordionDetails, AccordionSummary, Autocomplete, Box, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, Paper, Stack, TextField, Typography } from '@mui/material';
import ExpandMoreRoundedIcon from '@mui/icons-material/ExpandMoreRounded';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { appointmentsApi, customersApi } from '../api/modules';
import { ApiErrorAlert, apiErrorMessage } from '../components/ApiErrorAlert';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { DateInput, DateTimeInput } from '../components/DateTimeInputs';
import { FormTextField } from '../components/FormTextField';
import { LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';
import type { Appointment, Customer, Vehicle } from '../types';
import {
  fullCalendarSkopjeDateTimeInput,
  fullCalendarSkopjeOffsetDateTime,
  skopjeDate,
  skopjeDateTimeInput,
  skopjeDisplayDateTime,
  skopjeTime
} from '../utils/dateTime';

const createSchema = (messages: { required: string }) => z.object({
  customerId: z.string().min(1, messages.required),
  vehicleId: z.string().min(1, messages.required),
  title: z.string().min(1, messages.required),
  startsAt: z.string().regex(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/),
  endsAt: z.string().regex(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/),
  status: z.literal('SCHEDULED')
});

type AppointmentForm = z.output<ReturnType<typeof createSchema>>;
type EventDropArg = Parameters<NonNullable<ComponentProps<typeof FullCalendar>['eventDrop']>>[0];
type EventClickArg = Parameters<NonNullable<ComponentProps<typeof FullCalendar>['eventClick']>>[0];

const workingDayStartMinutes = 8 * 60;
const workingDayEndMinutes = 16 * 60;
const customerLabel = (customer: Customer | null) => customer?.name ?? '';
const vehicleLabel = (vehicle: Vehicle | null) =>
  vehicle ? `${vehicle.plate} - ${vehicle.make} ${vehicle.model}${vehicle.year ? ` (${vehicle.year})` : ''}` : '';
const appointmentVehicleLabel = (appointment: Appointment) =>
  [appointment.vehiclePlate, appointment.vehicleName].filter(Boolean).join(' - ') || appointment.vehicleId;
const localDateTimeParts = (value: string) => {
  const match = value.match(/^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})/);
  if (!match) return null;
  return {
    date: `${match[1]}-${match[2]}-${match[3]}`,
    minutesOfDay: Number(match[4]) * 60 + Number(match[5]),
    comparableMinutes: Date.UTC(Number(match[1]), Number(match[2]) - 1, Number(match[3]), Number(match[4]), Number(match[5])) / 60_000
  };
};

const overlaps = (start: number, end: number, appointment: Appointment) => {
  const appointmentStart = localDateTimeParts(skopjeDateTimeInput(appointment.startsAt));
  const appointmentEnd = localDateTimeParts(skopjeDateTimeInput(appointment.endsAt));
  return Boolean(appointmentStart && appointmentEnd && start < appointmentEnd.comparableMinutes && end > appointmentStart.comparableMinutes);
};

export function AppointmentsPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const [slotDate, setSlotDate] = useState(skopjeDate(new Date()));
  const [errorMessage, setErrorMessage] = useState('');
  const [selectedAppointment, setSelectedAppointment] = useState<Appointment | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<Appointment | null>(null);
  const schema = useMemo(
    () => createSchema({
      required: t('required')
    }),
    [t]
  );
  const { data = [], isLoading } = useQuery({
    queryKey: ['appointments'],
    queryFn: appointmentsApi.list,
    refetchInterval: 30_000,
    refetchOnWindowFocus: true
  });
  const availableQuery = useQuery({ queryKey: ['appointments', 'available', slotDate], queryFn: () => appointmentsApi.available(slotDate) });
  const createMutation = useMutation({ mutationFn: appointmentsApi.create });
  const deleteMutation = useMutation({ mutationFn: appointmentsApi.remove });
  const rescheduleMutation = useMutation({ mutationFn: ({ id, startsAt, endsAt }: { id: string; startsAt: string; endsAt: string }) => appointmentsApi.reschedule(id, startsAt, endsAt) });
  const { control, handleSubmit, reset, setValue, setError, clearErrors, formState } = useForm<AppointmentForm>({
    resolver: zodResolver(schema) as never,
    defaultValues: {
      customerId: '',
      vehicleId: '',
      title: 'Minor Service',
      startsAt: skopjeDateTimeInput(new Date()),
      endsAt: skopjeDateTimeInput(new Date(Date.now() + 60 * 60_000)),
      status: 'SCHEDULED'
    }
  });
  const selectedCustomerId = useWatch({ control, name: 'customerId' });
  const selectedVehicleId = useWatch({ control, name: 'vehicleId' });
  const customersQuery = useQuery({ queryKey: ['customers', 'appointment-form'], queryFn: () => customersApi.list() });
  const vehiclesQuery = useQuery({
    queryKey: ['customers', selectedCustomerId, 'vehicles', 'appointment-form'],
    queryFn: () => customersApi.vehicles(selectedCustomerId),
    enabled: Boolean(selectedCustomerId)
  });
  const customers = customersQuery.data ?? [];
  const vehicles = vehiclesQuery.data ?? [];
  const selectedCustomer = customers.find((customer) => String(customer.id) === selectedCustomerId) ?? null;
  const selectedVehicle = vehicles.find((vehicle) => String(vehicle.id) === selectedVehicleId) ?? null;
  const activeAppointments = useMemo(() => data.filter((appointment) => appointment.status !== 'CANCELLED'), [data]);

  const events = useMemo(
    () =>
      activeAppointments.map((appointment) => ({
          id: String(appointment.id),
          title: appointment.title,
          start: appointment.startsAt,
          end: appointment.endsAt,
          extendedProps: {
            status: appointment.status,
            tooltip: `${appointment.title} - ${appointment.customerName ?? appointment.customerId} - ${appointmentVehicleLabel(appointment)}`
          }
        })),
    [activeAppointments]
  );

  const onSubmit = handleSubmit(async (values) => {
    clearErrors(['startsAt', 'endsAt']);
    setErrorMessage('');
    const start = localDateTimeParts(values.startsAt);
    const end = localDateTimeParts(values.endsAt);
    if (!start || !end) return;
    if (end.comparableMinutes <= start.comparableMinutes) {
      setError('endsAt', { type: 'validate', message: t('appointmentEndAfterStart') });
      return;
    }
    if (start.date !== end.date) {
      setError('endsAt', { type: 'validate', message: t('appointmentSameDayRequired') });
      return;
    }
    if (start.minutesOfDay < workingDayStartMinutes || end.minutesOfDay > workingDayEndMinutes) {
      setError('startsAt', { type: 'validate', message: t('appointmentOutsideWorkingHours') });
      setError('endsAt', { type: 'validate', message: t('appointmentOutsideWorkingHours') });
      return;
    }
    if (activeAppointments.some((appointment) => overlaps(start.comparableMinutes, end.comparableMinutes, appointment))) {
      setError('startsAt', { type: 'validate', message: t('appointmentOverlap') });
      setError('endsAt', { type: 'validate', message: t('appointmentOverlap') });
      return;
    }
    try {
      await createMutation.mutateAsync(values);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['appointments'] }),
        queryClient.invalidateQueries({ queryKey: ['appointments', 'available'] })
      ]);
      reset({ ...values, title: '' });
      showToast(t('saved'));
    } catch (error) {
      const message = apiErrorMessage(error, t('appointmentSaveFailed'));
      setError('startsAt', { type: 'server', message });
      setError('endsAt', { type: 'server', message });
      setErrorMessage(message);
    }
  });

  const handleDrop = async (info: EventDropArg) => {
    const startsAt = info.event.start ? fullCalendarSkopjeOffsetDateTime(info.event.start) : undefined;
    const endsAt = info.event.end ? fullCalendarSkopjeOffsetDateTime(info.event.end) : fullCalendarSkopjeOffsetDateTime(new Date((info.event.start?.getTime() ?? Date.now()) + 60 * 60_000));
    if (!startsAt) {
      info.revert();
      return;
    }
    try {
      await rescheduleMutation.mutateAsync({ id: info.event.id, startsAt, endsAt });
      await queryClient.invalidateQueries({ queryKey: ['appointments'] });
      showToast(t('updated'));
    } catch {
      info.revert();
      showToast(t('rescheduleFailed'), 'error');
    }
  };

  const handleEventClick = (info: EventClickArg) => {
    info.jsEvent.preventDefault();
    const appointment = activeAppointments.find((item) => String(item.id) === String(info.event.id));
    if (appointment) {
      setSelectedAppointment(appointment);
    }
  };

  const deleteAppointment = async (appointment: Appointment) => {
    try {
      await deleteMutation.mutateAsync(appointment.id);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['appointments'] }),
        queryClient.invalidateQueries({ queryKey: ['appointments', 'available'] })
      ]);
      setDeleteTarget(null);
      setSelectedAppointment(null);
      showToast(t('deleted'));
    } catch (error) {
      showToast(apiErrorMessage(error, t('appointmentDeleteFailed')), 'error');
    }
  };

  if (isLoading) return <LoadingState />;

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} justifyContent="space-between">
        <Box>
          <Typography variant="h2">{t('appointments')}</Typography>
          <Typography color="text.secondary">{t('appointmentInstructions')}</Typography>
        </Box>
      </Stack>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', xl: '1.7fr 0.8fr' }, gap: 3 }}>
        <Paper sx={{ p: { xs: 2, md: 3 }, minWidth: 0, alignSelf: 'start' }}>
          <FullCalendar
            plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
            initialView="timeGridWeek"
            firstDay={1}
            headerToolbar={{ left: 'prev,next today', center: 'title', right: 'dayGridMonth,timeGridWeek,timeGridDay' }}
            events={events}
            editable
            selectable
            nowIndicator
            timeZone="Europe/Skopje"
            slotMinTime="06:00:00"
            slotMaxTime="18:00:00"
            scrollTime="08:00:00"
            businessHours={{
              daysOfWeek: [1, 2, 3, 4, 5, 6],
              startTime: '08:00',
              endTime: '16:00'
            }}
            slotLabelFormat={{ hour: '2-digit', minute: '2-digit', hour12: false, meridiem: false }}
            eventTimeFormat={{ hour: '2-digit', minute: '2-digit', hour12: false, meridiem: false }}
            height="auto"
            eventDrop={handleDrop}
            eventClick={handleEventClick}
            eventDidMount={(info) => {
              const tooltip = info.event.extendedProps.tooltip;
              if (typeof tooltip === 'string') {
                info.el.setAttribute('title', tooltip);
              }
              info.el.style.cursor = 'pointer';
            }}
            select={(selection) => {
              setValue('startsAt', fullCalendarSkopjeDateTimeInput(selection.start));
              setValue('endsAt', fullCalendarSkopjeDateTimeInput(selection.end));
            }}
          />
        </Paper>
        <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 }, alignSelf: 'start' }}>
          <Stack spacing={2.5}>
            <Typography variant="h4">{t('bookAppointment')}</Typography>
            <ApiErrorAlert message={errorMessage} />
            <DateInput
              name="slotDate"
              label={t('checkAvailabilityDate')}
              value={slotDate}
              error={false}
              onBlur={() => undefined}
              onChange={(value) => setSlotDate(value)}
              helperText="DD.MM.YYYY"
            />
            <Stack direction="row" flexWrap="wrap" gap={1}>
              {(availableQuery.data ?? []).map((slot) => (
                <Chip
                  key={slot.startsAt}
                  label={skopjeTime(slot.startsAt)}
                  onClick={() => {
                    setValue('startsAt', skopjeDateTimeInput(slot.startsAt));
                    setValue('endsAt', skopjeDateTimeInput(slot.endsAt));
                  }}
                />
              ))}
            </Stack>
            <Controller
              control={control}
              name="customerId"
              render={({ field, fieldState }) => (
                <Autocomplete
                  options={customers}
                  value={selectedCustomer}
                  loading={customersQuery.isLoading}
                  getOptionLabel={customerLabel}
                  isOptionEqualToValue={(option, value) => String(option.id) === String(value.id)}
                  onChange={(_, customer) => {
                    field.onChange(customer ? String(customer.id) : '');
                    setValue('vehicleId', '', { shouldDirty: true, shouldValidate: false });
                  }}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label={t('customer')}
                      inputRef={field.ref}
                      name={field.name}
                      onBlur={field.onBlur}
                      error={Boolean(fieldState.error)}
                      helperText={fieldState.error?.message}
                    />
                  )}
                />
              )}
            />
            <Controller
              control={control}
              name="vehicleId"
              render={({ field, fieldState }) => (
                <Autocomplete
                  options={vehicles}
                  value={selectedVehicle}
                  loading={vehiclesQuery.isLoading}
                  disabled={!selectedCustomerId}
                  getOptionLabel={vehicleLabel}
                  isOptionEqualToValue={(option, value) => String(option.id) === String(value.id)}
                  onChange={(_, vehicle) => field.onChange(vehicle ? String(vehicle.id) : '')}
                  renderInput={(params) => (
                    <TextField
                      {...params}
                      label={t('vehicle')}
                      inputRef={field.ref}
                      name={field.name}
                      onBlur={field.onBlur}
                      error={Boolean(fieldState.error)}
                      helperText={fieldState.error?.message ?? (!selectedCustomerId ? t('selectCustomerFirst') : undefined)}
                    />
                  )}
                />
              )}
            />
            <FormTextField control={control} name="title" label={t('serviceType')} />
            <Accordion disableGutters sx={{ boxShadow: 'none', border: '1px solid', borderColor: 'divider', '&:before': { display: 'none' } }}>
              <AccordionSummary expandIcon={<ExpandMoreRoundedIcon />}>
                <Typography fontWeight={700}>{t('advancedCustomTime')}</Typography>
              </AccordionSummary>
              <AccordionDetails>
                <Stack spacing={2}>
                  <Controller
                    control={control}
                    name="startsAt"
                    render={({ field, fieldState }) => (
                      <DateTimeInput
                        label={t('start')}
                        name={field.name}
                        value={field.value}
                        error={Boolean(fieldState.error)}
                        helperText={fieldState.error?.message ?? 'DD.MM.YYYY HH:mm'}
                        onBlur={field.onBlur}
                        onChange={field.onChange}
                        inputRef={field.ref}
                      />
                    )}
                  />
                  <Controller
                    control={control}
                    name="endsAt"
                    render={({ field, fieldState }) => (
                      <DateTimeInput
                        label={t('end')}
                        name={field.name}
                        value={field.value}
                        error={Boolean(fieldState.error)}
                        helperText={fieldState.error?.message ?? 'DD.MM.YYYY HH:mm'}
                        onBlur={field.onBlur}
                        onChange={field.onChange}
                        inputRef={field.ref}
                      />
                    )}
                  />
                </Stack>
              </AccordionDetails>
            </Accordion>
            <Button type="submit" variant="contained" disabled={formState.isSubmitting}>
              {t('save')}
            </Button>
          </Stack>
        </Paper>
      </Box>
      <Dialog open={Boolean(selectedAppointment)} onClose={() => setSelectedAppointment(null)} maxWidth="sm" fullWidth>
        <DialogTitle>{selectedAppointment?.title}</DialogTitle>
        <DialogContent>
          {selectedAppointment ? (
            <Stack spacing={2} sx={{ pt: 1 }}>
              <AppointmentDetail label={t('customer')} value={selectedAppointment.customerName ?? selectedAppointment.customerId} />
              <AppointmentDetail label={t('vehicle')} value={appointmentVehicleLabel(selectedAppointment)} />
              <AppointmentDetail label={t('start')} value={skopjeDisplayDateTime(selectedAppointment.startsAt)} />
              <AppointmentDetail label={t('end')} value={skopjeDisplayDateTime(selectedAppointment.endsAt)} />
              <AppointmentDetail label={t('status')} value={selectedAppointment.status} />
            </Stack>
          ) : null}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSelectedAppointment(null)}>{t('cancel')}</Button>
          <Button color="error" variant="contained" disabled={!selectedAppointment || deleteMutation.isPending} onClick={() => setDeleteTarget(selectedAppointment)}>
            {t('delete')}
          </Button>
        </DialogActions>
      </Dialog>
      <ConfirmDialog
        open={Boolean(deleteTarget)}
        title={t('deleteAppointment')}
        description={t('deleteAppointmentConfirm', { name: deleteTarget?.title ?? t('appointments') })}
        confirmLabel={t('delete')}
        cancelLabel={t('cancel')}
        confirming={deleteMutation.isPending}
        onCancel={() => setDeleteTarget(null)}
        onConfirm={() => deleteTarget ? deleteAppointment(deleteTarget) : undefined}
      />
    </Stack>
  );
}

function AppointmentDetail({ label, value }: { label: string; value: string }) {
  return (
    <Box>
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
      <Typography fontWeight={700}>{value}</Typography>
    </Box>
  );
}

import { useMemo, useState, type ComponentProps } from 'react';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import { zodResolver } from '@hookform/resolvers/zod';
import { Box, Button, Chip, Paper, Stack, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Controller, useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { appointmentsApi } from '../api/modules';
import { DateInput, DateTimeInput } from '../components/DateTimeInputs';
import { FormTextField } from '../components/FormTextField';
import { LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';
import {
  skopjeDate,
  skopjeDateTimeInput,
  skopjeOffsetDateTime,
  skopjeTime
} from '../utils/dateTime';

const schema = z.object({
  customerId: z.string().min(1),
  vehicleId: z.string().min(1),
  title: z.string().min(1),
  startsAt: z.string().regex(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/),
  endsAt: z.string().regex(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/),
  status: z.literal('BOOKED')
});

type AppointmentForm = z.output<typeof schema>;
type EventDropArg = Parameters<NonNullable<ComponentProps<typeof FullCalendar>['eventDrop']>>[0];

export function AppointmentsPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const [slotDate, setSlotDate] = useState(skopjeDate(new Date()));
  const { data = [], isLoading } = useQuery({ queryKey: ['appointments'], queryFn: appointmentsApi.list });
  const availableQuery = useQuery({ queryKey: ['appointments', 'available', slotDate], queryFn: () => appointmentsApi.available(slotDate) });
  const createMutation = useMutation({ mutationFn: appointmentsApi.create });
  const rescheduleMutation = useMutation({ mutationFn: ({ id, startsAt, endsAt }: { id: string; startsAt: string; endsAt: string }) => appointmentsApi.reschedule(id, startsAt, endsAt) });
  const { control, handleSubmit, reset, setValue, formState } = useForm<AppointmentForm>({
    resolver: zodResolver(schema) as never,
    defaultValues: {
      customerId: '',
      vehicleId: '',
      title: 'Minor Service',
      startsAt: skopjeDateTimeInput(new Date()),
      endsAt: skopjeDateTimeInput(new Date(Date.now() + 60 * 60_000)),
      status: 'BOOKED'
    }
  });

  const events = useMemo(
    () =>
      data.map((appointment) => ({
        id: appointment.id,
        title: appointment.title,
        start: appointment.startsAt,
        end: appointment.endsAt,
        extendedProps: { status: appointment.status }
      })),
    [data]
  );

  const onSubmit = handleSubmit(async (values) => {
    await createMutation.mutateAsync(values);
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['appointments'] }),
      queryClient.invalidateQueries({ queryKey: ['appointments', 'available'] })
    ]);
    reset({ ...values, title: '' });
    showToast(t('saved'));
  });

  const handleDrop = async (info: EventDropArg) => {
    const startsAt = info.event.start ? skopjeOffsetDateTime(info.event.start) : undefined;
    const endsAt = info.event.end ? skopjeOffsetDateTime(info.event.end) : skopjeOffsetDateTime(new Date((info.event.start?.getTime() ?? Date.now()) + 60 * 60_000));
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

  if (isLoading) return <LoadingState />;

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', md: 'row' }} spacing={2} justifyContent="space-between">
        <Box>
          <Typography variant="h2">{t('appointments')}</Typography>
          <Typography color="text.secondary">Drag appointments to reschedule. Conflicting changes are rejected and restored.</Typography>
        </Box>
      </Stack>
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', xl: '1.7fr 0.8fr' }, gap: 3 }}>
        <Paper sx={{ p: { xs: 2, md: 3 }, minWidth: 0 }}>
          <FullCalendar
            plugins={[dayGridPlugin, timeGridPlugin, interactionPlugin]}
            initialView="timeGridWeek"
            headerToolbar={{ left: 'prev,next today', center: 'title', right: 'dayGridMonth,timeGridWeek,timeGridDay' }}
            events={events}
            editable
            selectable
            nowIndicator
            timeZone="Europe/Skopje"
            slotLabelFormat={{ hour: '2-digit', minute: '2-digit', hour12: false }}
            eventTimeFormat={{ hour: '2-digit', minute: '2-digit', hour12: false }}
            height="auto"
            eventDrop={handleDrop}
            select={(selection) => {
              setValue('startsAt', skopjeDateTimeInput(selection.start));
              setValue('endsAt', skopjeDateTimeInput(selection.end));
            }}
          />
        </Paper>
        <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 }, alignSelf: 'start' }}>
          <Stack spacing={2.5}>
            <Typography variant="h4">{t('bookAppointment')}</Typography>
            <DateInput
              name="slotDate"
              label="Available date"
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
            <FormTextField control={control} name="customerId" label="Customer ID" />
            <FormTextField control={control} name="vehicleId" label="Vehicle ID" />
            <FormTextField control={control} name="title" label="Service type" />
            <Controller
              control={control}
              name="startsAt"
              render={({ field, fieldState }) => (
                <DateTimeInput
                  label="Start"
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
                  label="End"
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
            <Button type="submit" variant="contained" disabled={formState.isSubmitting}>
              {t('save')}
            </Button>
          </Stack>
        </Paper>
      </Box>
    </Stack>
  );
}

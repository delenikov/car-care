import { useMemo } from 'react';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin, { type EventDropArg } from '@fullcalendar/interaction';
import timeGridPlugin from '@fullcalendar/timegrid';
import { zodResolver } from '@hookform/resolvers/zod';
import { Box, Button, Paper, Stack, Typography } from '@mui/material';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import { appointmentsApi } from '../api/modules';
import { FormTextField } from '../components/FormTextField';
import { LoadingState } from '../components/LoadingState';
import { useToast } from '../components/ToastProvider';

const schema = z.object({
  customerId: z.string().min(1),
  vehicleId: z.string().min(1),
  title: z.string().min(1),
  startsAt: z.string().min(1),
  endsAt: z.string().min(1),
  status: z.literal('BOOKED')
});

type AppointmentForm = z.infer<typeof schema>;

const toLocalInput = (date: Date) => {
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
};

export function AppointmentsPage() {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const { showToast } = useToast();
  const { data = [], isLoading } = useQuery({ queryKey: ['appointments'], queryFn: appointmentsApi.list });
  const createMutation = useMutation({ mutationFn: appointmentsApi.create });
  const rescheduleMutation = useMutation({ mutationFn: ({ id, startsAt, endsAt }: { id: string; startsAt: string; endsAt: string }) => appointmentsApi.reschedule(id, startsAt, endsAt) });
  const { control, handleSubmit, reset, setValue, formState } = useForm<AppointmentForm>({
    resolver: zodResolver(schema),
    defaultValues: {
      customerId: '',
      vehicleId: '',
      title: '',
      startsAt: toLocalInput(new Date()),
      endsAt: toLocalInput(new Date(Date.now() + 60 * 60_000)),
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
    await queryClient.invalidateQueries({ queryKey: ['appointments'] });
    reset({ ...values, title: '' });
    showToast(t('saved'));
  });

  const handleDrop = async (info: EventDropArg) => {
    const startsAt = info.event.start?.toISOString();
    const endsAt = info.event.end?.toISOString() ?? new Date((info.event.start?.getTime() ?? Date.now()) + 60 * 60_000).toISOString();
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
          <Typography color="text.secondary">Повлечете термин за презакажување. При грешка, календарот автоматски се враќа назад.</Typography>
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
            height="auto"
            eventDrop={handleDrop}
            select={(selection) => {
              setValue('startsAt', toLocalInput(selection.start));
              setValue('endsAt', toLocalInput(selection.end));
            }}
          />
        </Paper>
        <Paper component="form" onSubmit={onSubmit} sx={{ p: { xs: 3, md: 4 }, alignSelf: 'start' }}>
          <Stack spacing={2.5}>
            <Typography variant="h4">{t('bookAppointment')}</Typography>
            <FormTextField control={control} name="customerId" label="ID на клиент" />
            <FormTextField control={control} name="vehicleId" label="ID на возило" />
            <FormTextField control={control} name="title" label="Опис" />
            <FormTextField control={control} name="startsAt" label="Почеток" type="datetime-local" InputLabelProps={{ shrink: true }} />
            <FormTextField control={control} name="endsAt" label="Крај" type="datetime-local" InputLabelProps={{ shrink: true }} />
            <Button type="submit" variant="contained" disabled={formState.isSubmitting}>
              {t('save')}
            </Button>
          </Stack>
        </Paper>
      </Box>
    </Stack>
  );
}

import dayjs, { type Dayjs } from 'dayjs';
import customParseFormat from 'dayjs/plugin/customParseFormat';
import 'dayjs/locale/mk';
import { Box } from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { TimePicker } from '@mui/x-date-pickers/TimePicker';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { useTranslation } from 'react-i18next';
import { skopjeDate, skopjeTime } from '../utils/dateTime';

dayjs.extend(customParseFormat);
dayjs.locale('mk');

const dateHelperText = 'DD.MM.YYYY';
const timeHelperText = 'HH:mm';

const parseDate = (value: string) => {
  const parsed = dayjs(value, 'YYYY-MM-DD', true);
  return parsed.isValid() ? parsed : null;
};

const parseTime = (value: string) => {
  const parsed = dayjs(value.slice(0, 5), 'HH:mm', true);
  return parsed.isValid() ? parsed : null;
};

const formatDate = (value: Dayjs | null) => (value?.isValid() ? value.format('YYYY-MM-DD') : '');
const formatTime = (value: Dayjs | null) => (value?.isValid() ? value.format('HH:mm') : '');
const datePart = (value: string) => value.split('T')[0] ?? skopjeDate(new Date());
const timePart = (value: string) => value.split('T')[1]?.slice(0, 5) ?? skopjeTime(new Date());
const combineDateTime = (date: string, time: string) => `${date}T${time}`;

interface BaseInputProps {
  error: boolean;
  helperText?: string;
  inputRef?: (element: HTMLInputElement | null) => void;
  label: string;
  name: string;
  onBlur: () => void;
}

export function DateInput({
  error,
  helperText,
  inputRef,
  label,
  name,
  onBlur,
  onChange,
  value
}: BaseInputProps & {
  onChange: (value: string) => void;
  value: string;
}) {
  return (
    <LocalizationProvider dateAdapter={AdapterDayjs} adapterLocale="mk">
      <DatePicker
        label={label}
        value={parseDate(value)}
        format="DD.MM.YYYY"
        onChange={(nextValue) => onChange(formatDate(nextValue))}
        slotProps={{
          textField: {
            fullWidth: true,
            name,
            error,
            helperText: helperText ?? dateHelperText,
            inputRef,
            onBlur
          }
        }}
      />
    </LocalizationProvider>
  );
}

export function TimeInput({
  error,
  helperText,
  inputRef,
  label,
  name,
  onBlur,
  onChange,
  value
}: BaseInputProps & {
  onChange: (value: string) => void;
  value: string;
}) {
  return (
    <LocalizationProvider dateAdapter={AdapterDayjs} adapterLocale="mk">
      <TimePicker
        label={label}
        value={parseTime(value)}
        ampm={false}
        views={['hours', 'minutes']}
        format="HH:mm"
        onChange={(nextValue) => onChange(formatTime(nextValue))}
        slotProps={{
          textField: {
            fullWidth: true,
            name,
            error,
            helperText: helperText ?? timeHelperText,
            inputRef,
            onBlur
          }
        }}
      />
    </LocalizationProvider>
  );
}

export function DateTimeInput({
  error,
  helperText,
  inputRef,
  label,
  name,
  onBlur,
  onChange,
  value
}: BaseInputProps & {
  onChange: (value: string) => void;
  value: string;
}) {
  const currentDate = parseDate(datePart(value));
  const currentTime = parseTime(timePart(value));
  const { t } = useTranslation();
  const dateLabel = `${label} ${t('date').toLocaleLowerCase()}`;
  const timeLabel = `${label} ${t('time').toLocaleLowerCase()}`;

  return (
    <LocalizationProvider dateAdapter={AdapterDayjs} adapterLocale="mk">
      <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 0.8fr' }, gap: 2 }}>
        <DatePicker
          label={dateLabel}
          value={currentDate}
          format="DD.MM.YYYY"
          onChange={(nextValue) => {
            const nextDate = formatDate(nextValue);
            onChange(nextDate ? combineDateTime(nextDate, formatTime(currentTime) || '00:00') : '');
          }}
          slotProps={{
            textField: {
              fullWidth: true,
              name: `${name}Date`,
              error,
              helperText: helperText ?? dateHelperText,
              inputRef,
              onBlur
            }
          }}
        />
        <TimePicker
          label={timeLabel}
          value={currentTime}
          ampm={false}
          views={['hours', 'minutes']}
          format="HH:mm"
          onChange={(nextValue) => {
            const nextTime = formatTime(nextValue);
            onChange(nextTime ? combineDateTime(formatDate(currentDate) || skopjeDate(new Date()), nextTime) : '');
          }}
          slotProps={{
            textField: {
              fullWidth: true,
              name: `${name}Time`,
              error,
              helperText: error ? helperText : timeHelperText,
              onBlur
            }
          }}
        />
      </Box>
    </LocalizationProvider>
  );
}

const SKOPJE_TIME_ZONE = 'Europe/Skopje';

const partsFor = (value: Date | string) => {
  if (typeof value === 'string') {
    const local = value.match(/^(\d{4})-(\d{2})-(\d{2})(?:T(\d{2}):(\d{2})(?::(\d{2}))?)?$/);
    if (local) {
      return {
        year: local[1],
        month: local[2],
        day: local[3],
        hour: local[4] ?? '00',
        minute: local[5] ?? '00',
        second: local[6] ?? '00'
      };
    }
  }
  const date = value instanceof Date ? value : new Date(value);
  const parts = new Intl.DateTimeFormat('en-GB', {
    timeZone: SKOPJE_TIME_ZONE,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).formatToParts(date);
  return Object.fromEntries(parts.map((part) => [part.type, part.value]));
};

const offsetFor = (date: Date) => {
  const timeZoneName = new Intl.DateTimeFormat('en-GB', {
    timeZone: SKOPJE_TIME_ZONE,
    timeZoneName: 'shortOffset'
  }).formatToParts(date).find((part) => part.type === 'timeZoneName')?.value ?? 'GMT+1';
  const match = timeZoneName.match(/GMT([+-])(\d{1,2})(?::(\d{2}))?/);
  if (!match) {
    return '+01:00';
  }
  const [, sign, hours, minutes = '00'] = match;
  return `${sign}${hours.padStart(2, '0')}:${minutes}`;
};

export const skopjeDate = (value: Date | string) => {
  const parts = partsFor(value);
  return `${parts.year}-${parts.month}-${parts.day}`;
};

export const skopjeTime = (value: Date | string) => {
  const parts = partsFor(value);
  return `${parts.hour}:${parts.minute}:${parts.second}`;
};

export const skopjeDateTimeInput = (value: Date | string) => `${skopjeDate(value)}T${skopjeTime(value)}`;

export const skopjeDisplayDate = (value: Date | string) => {
  const parts = partsFor(value);
  return `${parts.day}.${parts.month}.${parts.year}.`;
};

export const skopjeDisplayDateTime = (value: Date | string) => `${skopjeDisplayDate(value)} ${skopjeTime(value)}`;

export const parseSkopjeDisplayDate = (value: string) => {
  const match = value.trim().match(/^(\d{2})\.(\d{2})\.(\d{4})\.?$/);
  return match ? `${match[3]}-${match[2]}-${match[1]}` : null;
};

export const parseSkopjeDisplayDateTime = (value: string) => {
  const match = value.trim().match(/^(\d{2})\.(\d{2})\.(\d{4})\.?\s+(\d{2}):(\d{2}):(\d{2})$/);
  return match ? `${match[3]}-${match[2]}-${match[1]}T${match[4]}:${match[5]}:${match[6]}` : null;
};

export const skopjeOffsetDateTime = (value: Date | string): string => {
  if (value instanceof Date) {
    return `${skopjeDateTimeInput(value)}${offsetFor(value)}`;
  }
  if (/[zZ]|[+-]\d{2}:?\d{2}$/.test(value)) {
    return skopjeOffsetDateTime(new Date(value));
  }
  const [datePart, timePart = '00:00:00'] = value.split('T');
  const normalizedTime = timePart.length === 5 ? `${timePart}:00` : timePart;
  const utcGuess = new Date(`${datePart}T${normalizedTime}Z`);
  return `${datePart}T${normalizedTime}${offsetFor(utcGuess)}`;
};

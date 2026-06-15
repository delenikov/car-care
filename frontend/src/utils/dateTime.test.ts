import { describe, expect, it } from 'vitest';
import { fullCalendarBusinessDateTimeInput, fullCalendarBusinessOffsetDateTime } from './dateTime';

describe('dateTime FullCalendar helpers', () => {
  it('keeps the visible FullCalendar wall-clock time when creating a Skopje offset timestamp', () => {
    const droppedStart = new Date(Date.UTC(2026, 5, 20, 13, 30));
    const droppedEnd = new Date(Date.UTC(2026, 5, 20, 16, 0));

    expect(fullCalendarBusinessOffsetDateTime(droppedStart)).toBe('2026-06-20T13:30:00+02:00');
    expect(fullCalendarBusinessOffsetDateTime(droppedEnd)).toBe('2026-06-20T16:00:00+02:00');
  });

  it('keeps the visible FullCalendar wall-clock time for date-time picker inputs', () => {
    const selectedSlot = new Date(Date.UTC(2026, 5, 20, 8, 0));

    expect(fullCalendarBusinessDateTimeInput(selectedSlot)).toBe('2026-06-20T08:00');
  });
});

import { useEffect, useMemo, useRef, useState } from 'react';
import WheelColumn from './WheelColumn';
import type { WheelOption } from './WheelColumn';
import { clinicDetails, clinicSlotMinutes } from '../../utils/clinic';
import type { ClinicLocation } from '../../utils/clinic';

interface WheelSlotPickerProps {
  value: string;
  onChange: (value: string) => void;
  clinicLocation: ClinicLocation;
}

interface ClinicDate {
  key: string;
  date: Date;
  label: string;
}

function dateKey(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function buildDates(): ClinicDate[] {
  const today = new Date();
  today.setHours(12, 0, 0, 0);
  const dates: ClinicDate[] = [];
  for (let offset = 0; dates.length < 6; offset += 1) {
    const date = new Date(today);
    date.setDate(today.getDate() + offset);
    if (date.getDay() === 0) continue;
    const label = offset === 0 ? 'Today' : offset === 1 ? 'Tomorrow' : new Intl.DateTimeFormat('en-IN', {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
    }).format(date);
    dates.push({ key: dateKey(date), date, label });
  }
  return dates;
}

function slotLabel(minutes: number): string {
  const hours24 = Math.floor(minutes / 60);
  const minute = minutes % 60;
  const period = hours24 < 12 ? 'AM' : 'PM';
  const hours12 = hours24 % 12 || 12;
  return `${String(hours12).padStart(2, '0')}:${String(minute).padStart(2, '0')} ${period}`;
}

function slotsForDate(date: ClinicDate, clinicLocation: ClinicLocation): WheelOption[] {
  const now = new Date();
  return clinicSlotMinutes(clinicLocation).map((minutes) => {
    const hour = Math.floor(minutes / 60);
    const minute = minutes % 60;
    const localDateTime = new Date(date.date);
    localDateTime.setHours(hour, minute, 0, 0);
    return {
      value: `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`,
      label: slotLabel(minutes),
      disabled: localDateTime.getTime() <= now.getTime(),
    };
  });
}

function defaultIndices(dates: ClinicDate[], clinicLocation: ClinicLocation): { dateIndex: number; timeIndex: number } {
  const dateIndex = dates.findIndex((date) => slotsForDate(date, clinicLocation).some((slot) => !slot.disabled));
  const safeDateIndex = Math.max(dateIndex, 0);
  const timeIndex = Math.max(slotsForDate(dates[safeDateIndex]!, clinicLocation).findIndex((slot) => !slot.disabled), 0);
  return { dateIndex: safeDateIndex, timeIndex };
}

export default function WheelSlotPicker({ value, onChange, clinicLocation }: WheelSlotPickerProps) {
  const dates = useMemo(buildDates, []);
  const initial = useMemo(() => defaultIndices(dates, clinicLocation), [clinicLocation, dates]);
  const [dateIndex, setDateIndex] = useState(initial.dateIndex);
  const [timeIndex, setTimeIndex] = useState(initial.timeIndex);
  const initialized = useRef(false);
  const previousClinicLocation = useRef(clinicLocation);

  const dateOptions = useMemo<WheelOption[]>(() => dates.map((date) => ({
    value: date.key,
    label: date.label,
    disabled: slotsForDate(date, clinicLocation).every((slot) => slot.disabled),
  })), [clinicLocation, dates]);
  const selectedDate = dates[dateIndex] ?? dates[0]!;
  const timeOptions = useMemo(() => slotsForDate(selectedDate, clinicLocation), [clinicLocation, selectedDate]);

  useEffect(() => {
    if (initialized.current) return;
    initialized.current = true;
    if (value) {
      const [datePart, timePart] = value.split('T');
      const foundDate = dates.findIndex((date) => date.key === datePart);
      if (foundDate >= 0) {
        setDateIndex(foundDate);
        const foundTime = slotsForDate(dates[foundDate]!, clinicLocation).findIndex((slot) => slot.value === timePart);
        if (foundTime >= 0 && !slotsForDate(dates[foundDate]!, clinicLocation)[foundTime]?.disabled) {
          setTimeIndex(foundTime);
          return;
        }
      }
    }

    const date = dates[dateIndex]!;
    const slot = timeOptions[timeIndex];
    if (!slot || slot.disabled) return;
    onChange(`${date.key}T${slot.value}`);
  }, [clinicLocation]);

  useEffect(() => {
    if (previousClinicLocation.current === clinicLocation) return;
    previousClinicLocation.current = clinicLocation;
    const next = defaultIndices(dates, clinicLocation);
    setDateIndex(next.dateIndex);
    setTimeIndex(next.timeIndex);
    const date = dates[next.dateIndex]!;
    const slot = slotsForDate(date, clinicLocation)[next.timeIndex];
    if (slot && !slot.disabled) onChange(`${date.key}T${slot.value}`);
  }, [clinicLocation, dates, onChange]);

  function chooseDate(index: number) {
    setDateIndex(index);
    const nextOptions = slotsForDate(dates[index]!, clinicLocation);
    const currentTime = timeOptions[timeIndex]?.value;
    const sameSlot = nextOptions.findIndex((slot) => slot.value === currentTime && !slot.disabled);
    const firstAvailable = nextOptions.findIndex((slot) => !slot.disabled);
    const nextTimeIndex = sameSlot >= 0 ? sameSlot : Math.max(firstAvailable, 0);
    setTimeIndex(nextTimeIndex);
    onChange(`${dates[index]!.key}T${nextOptions[nextTimeIndex]!.value}`);
  }

  function chooseTime(index: number) {
    const slot = timeOptions[index];
    if (!slot || slot.disabled) return;
    setTimeIndex(index);
    onChange(`${selectedDate.key}T${slot.value}`);
  }

  return (
    <fieldset className="min-w-0">
      <legend className="text-sm font-semibold text-clinic-ink">Choose a preferred appointment time</legend>
      <p className="mt-1 text-xs text-slate-500">{clinicDetails[clinicLocation].hours}.</p>
      <div className="wheel-picker-shell relative mt-4 grid grid-cols-[1fr_1.1fr] gap-3 rounded-2xl bg-clinic-paper p-3 sm:gap-6 sm:p-5">
        <WheelColumn label="Date" options={dateOptions} selectedIndex={dateIndex} onSelect={chooseDate} />
        <WheelColumn label="Shift time" options={timeOptions} selectedIndex={timeIndex} onSelect={chooseTime} />
      </div>
      <p className="mt-2 text-center text-xs font-semibold text-clinic-green">
        {timeOptions[timeIndex]?.value && Number(timeOptions[timeIndex]!.value.slice(0, 2)) < 15
          ? 'Morning shift'
          : 'Afternoon / evening shift'}
      </p>
      <p className="mt-2 text-xs text-slate-500">Open Monday–Saturday; closed Sundays. Past times and off-shift hours can’t be selected.</p>
    </fieldset>
  );
}

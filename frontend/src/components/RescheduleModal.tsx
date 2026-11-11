import { useState } from 'react';
import type { FormEvent } from 'react';
import { CalendarClock, X } from 'lucide-react';
import { updateAppointmentStatus } from '../api/client';
import type { AppointmentView } from '../types';
import WheelSlotPicker from './intake/WheelSlotPicker';
import { clinicDetails } from '../utils/clinic';

interface RescheduleModalProps {
  appointment: AppointmentView;
  onClose: () => void;
  onRescheduled: () => void;
}

function toLocalInput(value: string): string {
  if (!/(Z|[+-]\d{2}:\d{2})$/.test(value)) return value.slice(0, 16);
  const date = new Date(value);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
}

export default function RescheduleModal({ appointment, onClose, onRescheduled }: RescheduleModalProps) {
  const [time, setTime] = useState(toLocalInput(appointment.appointmentTime));
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setError('');
    try {
      await updateAppointmentStatus(appointment.id, { status: 'RESCHEDULED', rescheduledTime: time });
      onRescheduled();
    } catch {
      setError('The appointment could not be rescheduled. Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="modal-backdrop fixed inset-0 z-50 grid place-items-center bg-clinic-ink/45 p-4">
      <section role="dialog" aria-modal="true" aria-labelledby="reschedule-title" className="w-full max-w-md rounded-3xl bg-white p-6 shadow-2xl sm:p-8">
        <div className="flex items-start justify-between">
          <div>
            <span className="grid h-11 w-11 place-items-center rounded-2xl bg-clinic-mint text-clinic-green"><CalendarClock size={22} /></span>
            <h2 id="reschedule-title" className="mt-4 text-xl font-bold text-clinic-ink">Choose a new time</h2>
            <p className="mt-1 text-sm text-slate-500">For {appointment.patientName}</p>
            <p className="mt-1 text-xs text-slate-500">{clinicDetails[appointment.clinicLocation].label}</p>
          </div>
          <button aria-label="Close dialog" onClick={onClose} className="rounded-full p-2 text-slate-400 hover:bg-slate-100"><X size={19} /></button>
        </div>
        <form onSubmit={submit} className="mt-6 space-y-4">
          <WheelSlotPicker
            value={time}
            onChange={setTime}
            clinicLocation={appointment.clinicLocation}
          />
          {error && <p role="alert" className="text-sm text-red-600">{error}</p>}
          <button disabled={submitting} className="w-full rounded-xl bg-clinic-green px-4 py-3 font-semibold text-white hover:bg-clinic-ink disabled:opacity-60">
            {submitting ? 'Saving…' : 'Confirm new time'}
          </button>
        </form>
      </section>
    </div>
  );
}

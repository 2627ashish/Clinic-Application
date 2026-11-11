import { useCallback, useEffect, useMemo, useState } from 'react';
import { CalendarDays, Check, Clock3, LoaderCircle, MessageCircle, RefreshCw, Stethoscope, Users, Video, XCircle } from 'lucide-react';
import { getAppointments, updateAppointmentStatus } from '../api/client';
import BroadcastCard from '../components/BroadcastCard';
import ConsultationModal from '../components/ConsultationModal';
import RescheduleModal from '../components/RescheduleModal';
import type { AppointmentStatus, AppointmentView } from '../types';
import { clinicDetails } from '../utils/clinic';
import type { ConsultationType } from '../types/appointment';

const statusStyles: Record<AppointmentStatus, string> = {
  PENDING_DOCTOR_APPROVAL: 'bg-amber-50 text-amber-700',
  CONFIRMED: 'bg-emerald-50 text-emerald-700',
  RESCHEDULED: 'bg-sky-50 text-sky-700',
  COMPLETED: 'bg-slate-100 text-slate-600',
  CANCELLED: 'bg-rose-50 text-rose-700',
};

function formatAppointmentTime(value: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

function whatsappCallUrl(phoneNumber: string, patientName: string): string | null {
  const phone = phoneNumber.replace(/\D/g, '');
  if (!phone) return null;
  const message = `Hello ${patientName}, Dr. B.S. Chauhan is ready for your scheduled online consultation.`;
  return `https://wa.me/${phone}?text=${encodeURIComponent(message)}`;
}

interface DoctorDashboardProps {
  onOpenSettings: () => void;
  onLogout: () => void;
}

export default function DoctorDashboard({ onOpenSettings, onLogout }: DoctorDashboardProps) {
  const [appointments, setAppointments] = useState<AppointmentView[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState<AppointmentView | null>(null);
  const [modal, setModal] = useState<'reschedule' | 'consultation' | null>(null);
  const [updatingId, setUpdatingId] = useState('');
  const [appointmentTab, setAppointmentTab] = useState<ConsultationType>('IN_CLINIC');
  const pendingCount = useMemo(() => appointments.filter((item) => item.status === 'PENDING_DOCTOR_APPROVAL').length, [appointments]);
  const confirmedCount = useMemo(() => appointments.filter((item) => item.status === 'CONFIRMED' || item.status === 'RESCHEDULED').length, [appointments]);

  const refresh = useCallback(async () => {
    setError('');
    try {
      const results = await getAppointments();
      setAppointments(results.sort((left, right) => left.appointmentTime.localeCompare(right.appointmentTime)));
    } catch {
      setError('Appointments could not be loaded. Make sure the API and database are running.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function confirmAppointment(appointment: AppointmentView) {
    setUpdatingId(appointment.id);
    setError('');
    try {
      await updateAppointmentStatus(appointment.id, { status: 'CONFIRMED' });
      await refresh();
    } catch {
      setError(`Could not confirm ${appointment.patientName}'s appointment.`);
    } finally {
      setUpdatingId('');
    }
  }

  async function markUnavailable(appointment: AppointmentView) {
    const confirmed = window.confirm(
      `Mark ${appointment.patientName}'s appointment as unavailable? They will receive WhatsApp rescheduling instructions.`,
    );
    if (!confirmed) return;
    setUpdatingId(appointment.id);
    setError('');
    try {
      await updateAppointmentStatus(appointment.id, { status: 'CANCELLED' });
      await refresh();
    } catch {
      setError(`Could not notify ${appointment.patientName} about rescheduling.`);
    } finally {
      setUpdatingId('');
    }
  }

  function openModal(appointment: AppointmentView, nextModal: 'reschedule' | 'consultation') {
    setSelected(appointment);
    setModal(nextModal);
  }

  async function closeAndRefresh() {
    setModal(null);
    setSelected(null);
    await refresh();
  }

  const activeAppointments = appointments.filter((item) =>
    item.status === 'PENDING_DOCTOR_APPROVAL' || item.status === 'CONFIRMED' || item.status === 'RESCHEDULED',
  );
  const tabAppointments = activeAppointments.filter((item) =>
    appointmentTab === 'ONLINE' ? item.consultationType === 'ONLINE' : item.consultationType !== 'ONLINE',
  );

  return (
    <main className="mx-auto max-w-7xl px-5 py-8 sm:px-8 sm:py-10">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.16em] text-clinic-green">Doctor portal</p>
          <h1 className="mt-2 text-3xl font-bold tracking-tight text-clinic-ink sm:text-4xl">Good day, Doctor</h1>
          <p className="mt-2 text-sm text-slate-500">Your clinic at a glance. Review requests and care for your patients.</p>
        </div>
        <div className="flex flex-wrap gap-2 self-start sm:self-auto">
          <button onClick={onOpenSettings} className="min-h-11 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-clinic-ink hover:border-clinic-green/30">Settings / Change Password</button>
          <button onClick={onLogout} className="min-h-11 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-clinic-ink hover:border-clinic-green/30">Sign out</button>
          <button onClick={() => void refresh()} className="inline-flex min-h-11 items-center gap-2 rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-clinic-ink hover:border-clinic-green/30"><RefreshCw size={15} /> Refresh</button>
        </div>
      </div>

      <div className="mt-7 grid gap-4 sm:grid-cols-3">
        {[
          { label: 'Total patients', value: appointments.length, icon: Users, tone: 'bg-clinic-mint text-clinic-green' },
          { label: 'Needs your review', value: pendingCount, icon: Clock3, tone: 'bg-amber-50 text-amber-700' },
          { label: 'Upcoming visits', value: confirmedCount, icon: CalendarDays, tone: 'bg-sky-50 text-sky-700' },
        ].map(({ label, value, icon: Icon, tone }) => (
          <article key={label} className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
            <div className="flex items-center justify-between"><p className="text-sm font-medium text-slate-500">{label}</p><span className={`grid h-10 w-10 place-items-center rounded-xl ${tone}`}><Icon size={19} /></span></div>
            <p className="mt-3 text-3xl font-bold text-clinic-ink">{value}</p>
          </article>
        ))}
      </div>

      {error && <p role="alert" className="mt-5 rounded-xl border border-red-100 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}

      <section className="mt-8 overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-sm">
        <div className="flex items-center justify-between border-b border-slate-100 px-5 py-5 sm:px-6">
          <div><h2 className="text-lg font-bold text-clinic-ink">Appointment requests</h2><p className="mt-1 text-sm text-slate-500">Review and manage the upcoming care schedule</p></div>
          <span className="rounded-full bg-clinic-paper px-3 py-1 text-xs font-semibold text-slate-600">{tabAppointments.length} active</span>
        </div>
        <div className="flex gap-2 border-b border-slate-100 px-5 py-3 sm:px-6" role="tablist" aria-label="Appointment type">
          {([
            ['IN_CLINIC', '🏥 In-Clinic Appointments'],
            ['ONLINE', '💻 Online Consultations'],
          ] as const).map(([type, label]) => (
            <button key={type} type="button" role="tab" aria-selected={appointmentTab === type}
              onClick={() => setAppointmentTab(type)}
              className={`rounded-xl px-4 py-2.5 text-sm font-semibold ${appointmentTab === type ? 'bg-clinic-green text-white' : 'bg-clinic-paper text-slate-600 hover:bg-clinic-mint'}`}>
              {label}
            </button>
          ))}
        </div>
        {loading ? (
          <div className="flex items-center justify-center gap-2 py-16 text-sm text-slate-500"><LoaderCircle className="animate-spin" size={18} /> Loading appointments…</div>
        ) : tabAppointments.length === 0 ? (
          <div className="px-6 py-16 text-center"><span className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-clinic-paper text-clinic-green"><Stethoscope size={22} /></span><p className="mt-4 font-semibold text-clinic-ink">All caught up</p><p className="mt-1 text-sm text-slate-500">New {appointmentTab === 'ONLINE' ? 'online consultation' : 'in-clinic appointment'} requests will show up here.</p></div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[760px] text-left">
              <thead className="bg-clinic-paper/70 text-xs uppercase tracking-wide text-slate-500"><tr><th className="px-6 py-3 font-semibold">Patient</th><th className="px-6 py-3 font-semibold">Visit</th><th className="px-6 py-3 font-semibold">Status</th><th className="px-6 py-3 text-right font-semibold">Actions</th></tr></thead>
              <tbody className="divide-y divide-slate-100">
                {tabAppointments.map((appointment) => (
                  <tr key={appointment.id} className="align-top hover:bg-clinic-paper/40">
                    <td className="px-6 py-4"><p className="font-semibold text-clinic-ink">{appointment.patientName}</p><p className="mt-1 text-xs text-slate-500">{appointment.phoneNumber}</p><p className="mt-2 max-w-xs text-sm text-slate-600">{appointment.chiefComplaintSummary}</p></td>
                    <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-600">
                      <p>{formatAppointmentTime(appointment.appointmentTime)}</p>
                      {appointmentTab === 'ONLINE'
                        ? <span className="mt-1 inline-flex rounded-full bg-indigo-50 px-2 py-1 text-[10px] font-bold uppercase tracking-wide text-indigo-700">Online consultation</span>
                        : <p className="mt-1 text-xs text-slate-500">{clinicDetails[appointment.clinicLocation].label}</p>}
                    </td>
                    <td className="px-6 py-4"><span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ${statusStyles[appointment.status]}`}>{appointment.status.replaceAll('_', ' ').toLowerCase()}</span></td>
                    <td className="px-6 py-4"><div className="flex justify-end gap-2">
                      {appointment.status === 'PENDING_DOCTOR_APPROVAL' && <button disabled={updatingId === appointment.id} onClick={() => void confirmAppointment(appointment)} className="inline-flex items-center gap-1.5 rounded-lg bg-clinic-green px-3 py-2 text-xs font-semibold text-white hover:bg-clinic-ink disabled:opacity-50"><Check size={14} />{appointmentTab === 'ONLINE' ? 'Approve & Schedule' : 'Confirm'}</button>}
                      {appointmentTab === 'ONLINE' && appointment.status !== 'PENDING_DOCTOR_APPROVAL' && whatsappCallUrl(appointment.phoneNumber, appointment.patientName) && (
                        <a href={whatsappCallUrl(appointment.phoneNumber, appointment.patientName) ?? undefined} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-2 text-xs font-semibold text-white hover:bg-emerald-700"><MessageCircle size={14} /> Start WhatsApp Call</a>
                      )}
                      {appointmentTab === 'ONLINE' && appointment.meetingLink && (
                        <a href={appointment.meetingLink} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1.5 rounded-lg border border-indigo-200 px-3 py-2 text-xs font-semibold text-indigo-700 hover:bg-indigo-50"><Video size={14} /> Open Video Room</a>
                      )}
                      <button disabled={updatingId === appointment.id} onClick={() => void markUnavailable(appointment)} className="inline-flex items-center gap-1.5 rounded-lg border border-rose-200 px-3 py-2 text-xs font-semibold text-rose-700 hover:bg-rose-50 disabled:opacity-50"><XCircle size={14} /> Unavailable</button>
                      {appointmentTab === 'IN_CLINIC' && <button onClick={() => openModal(appointment, 'reschedule')} className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold text-clinic-ink hover:bg-slate-50">Reschedule</button>}
                      <button onClick={() => openModal(appointment, 'consultation')} className="rounded-lg border border-clinic-green/20 bg-clinic-mint px-3 py-2 text-xs font-semibold text-clinic-green hover:bg-emerald-100">{appointmentTab === 'ONLINE' ? 'Write Rx & Bill' : 'Consult & bill'}</button>
                    </div></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <div className="mt-8"><BroadcastCard /></div>
      {selected && modal === 'reschedule' && <RescheduleModal appointment={selected} onClose={() => setModal(null)} onRescheduled={() => void closeAndRefresh()} />}
      {selected && modal === 'consultation' && <ConsultationModal appointment={selected} onClose={() => setModal(null)} onCompleted={() => void closeAndRefresh()} />}
    </main>
  );
}

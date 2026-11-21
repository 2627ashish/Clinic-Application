import { useCallback, useEffect, useMemo, useState } from 'react';
import { CalendarDays, Clock3, LoaderCircle, RefreshCw, Stethoscope, Users } from 'lucide-react';
import { deleteAppointment, getAppointments, updateAppointmentStatus } from '../api/client';
import BroadcastCard from '../components/BroadcastCard';
import ConsultationModal from '../components/ConsultationModal';
import RescheduleModal from '../components/RescheduleModal';
import WalkInIntakeCard from '../components/WalkInIntakeCard';
import AppointmentList from '../components/doctor/AppointmentList';
import MobileAppointmentDrawer from '../components/doctor/MobileAppointmentDrawer';
import type { AppointmentView } from '../types';
import type { ConsultationType } from '../types/appointment';

const PAGE_SIZE = 10;
const ACTIVE_STATUSES = 'PENDING_DOCTOR_APPROVAL,CONFIRMED,RESCHEDULED';

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
  const [appointmentTab, setAppointmentTab] = useState<ConsultationType | 'WALK_IN'>('IN_CLINIC');
  const [mobileSelected, setMobileSelected] = useState<AppointmentView | null>(null);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const pendingCount = useMemo(() => appointments.filter((item) => item.status === 'PENDING_DOCTOR_APPROVAL').length, [appointments]);
  const confirmedCount = useMemo(() => appointments.filter((item) => item.status === 'CONFIRMED' || item.status === 'RESCHEDULED').length, [appointments]);

  const refresh = useCallback(async () => {
    setError('');
    setLoading(true);
    try {
      const results = await getAppointments(
        page,
        PAGE_SIZE,
        appointmentTab === 'WALK_IN' ? 'IN_CLINIC' : appointmentTab,
        appointmentTab === 'WALK_IN' ? undefined : ACTIVE_STATUSES,
      );
      setAppointments(results.content);
      setTotalElements(results.totalElements);
      setTotalPages(Math.max(results.totalPages, 1));
      if (page >= results.totalPages && page > 0) setPage(Math.max(results.totalPages - 1, 0));
    } catch {
      setError('Appointments could not be loaded. Make sure the API and database are running.');
    } finally {
      setLoading(false);
    }
  }, [appointmentTab, page]);

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

  async function removeAppointment(appointment: AppointmentView) {
    if (!window.confirm('Are you sure you want to delete this appointment?')) return;
    setUpdatingId(appointment.id);
    setError('');
    try {
      await deleteAppointment(appointment.id);
      setMobileSelected(null);
      await refresh();
    } catch {
      setError(`Could not delete ${appointment.patientName}'s appointment. Clinical records may be attached.`);
    } finally {
      setUpdatingId('');
    }
  }

  function openModal(appointment: AppointmentView, nextModal: 'reschedule' | 'consultation') {
    setMobileSelected(null);
    setSelected(appointment);
    setModal(nextModal);
  }

  async function closeAndRefresh() {
    setModal(null);
    setSelected(null);
    await refresh();
  }

  async function openWalkInConsultation(appointmentId: string) {
    setError('');
    try {
      const results = await getAppointments(page, PAGE_SIZE, 'IN_CLINIC');
      const appointment = results.content.find((item) => item.id === appointmentId);
      setAppointments(results.content);
      setTotalPages(Math.max(results.totalPages, 1));
      setTotalElements(results.totalElements);
      if (!appointment) {
        setError('Walk-in record was created, but its appointment details could not be loaded.');
        return;
      }
      setSelected(appointment);
      setModal('consultation');
    } catch {
      setError('The walk-in record was created, but appointments could not be refreshed.');
    }
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
          { label: 'Total appointments', value: totalElements, icon: Users, tone: 'bg-clinic-mint text-clinic-green' },
          { label: 'Needs review (page)', value: pendingCount, icon: Clock3, tone: 'bg-amber-50 text-amber-700' },
          { label: 'Upcoming (page)', value: confirmedCount, icon: CalendarDays, tone: 'bg-sky-50 text-sky-700' },
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
          <span className="rounded-full bg-clinic-paper px-3 py-1 text-xs font-semibold text-slate-600">{appointmentTab === 'WALK_IN' ? appointments.filter((item) => item.intakeSource && new Date(item.appointmentTime).toDateString() === new Date().toDateString()).length : tabAppointments.length} {appointmentTab === 'WALK_IN' ? 'today' : 'active'}</span>
        </div>
        <div className="flex flex-wrap gap-2 border-b border-slate-100 px-5 py-3 sm:px-6" role="tablist" aria-label="Doctor intake">
          {([
            ['IN_CLINIC', '🏥 In-Clinic Appointments'],
            ['ONLINE', '💻 Online Consultations'],
            ['WALK_IN', 'Walk-In & Phone Intake'],
          ] as const).map(([type, label]) => (
            <button key={type} type="button" role="tab" aria-selected={appointmentTab === type}
              onClick={() => { setAppointmentTab(type); setPage(0); }}
              className={`rounded-xl px-4 py-2.5 text-sm font-semibold ${appointmentTab === type ? 'bg-clinic-green text-white' : 'bg-clinic-paper text-slate-600 hover:bg-clinic-mint'}`}>
              {label}
            </button>
          ))}
        </div>
        {appointmentTab === 'WALK_IN' ? (
          <WalkInIntakeCard appointments={appointments} onStartConsultation={(id) => void openWalkInConsultation(id)} />
        ) : loading ? (
          <div className="flex items-center justify-center gap-2 py-16 text-sm text-slate-500"><LoaderCircle className="animate-spin" size={18} /> Loading appointments…</div>
        ) : tabAppointments.length === 0 ? (
          <div className="px-6 py-16 text-center"><span className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-clinic-paper text-clinic-green"><Stethoscope size={22} /></span><p className="mt-4 font-semibold text-clinic-ink">All caught up</p><p className="mt-1 text-sm text-slate-500">New {appointmentTab === 'ONLINE' ? 'online consultation' : 'in-clinic appointment'} requests will show up here.</p></div>
        ) : (
          <AppointmentList
            appointments={tabAppointments}
            consultationType={appointmentTab}
            updatingId={updatingId}
            onConfirm={(appointment) => void confirmAppointment(appointment)}
            onUnavailable={(appointment) => void markUnavailable(appointment)}
            onReschedule={(appointment) => openModal(appointment, 'reschedule')}
            onConsult={(appointment) => openModal(appointment, 'consultation')}
            onDelete={(appointment) => void removeAppointment(appointment)}
            onSelectMobile={setMobileSelected}
          />
        )}
        {totalPages > 1 && (
          <nav className="flex items-center justify-between border-t border-slate-100 px-4 py-4 sm:px-6" aria-label="Appointment pages">
            <button type="button" disabled={page === 0 || loading} onClick={() => setPage((current) => Math.max(0, current - 1))}
              className="min-h-10 rounded-lg border border-slate-200 px-4 text-sm font-semibold text-slate-700 disabled:opacity-40">Previous</button>
            <p className="text-sm font-medium text-slate-600">Page {page + 1} of {totalPages}</p>
            <button type="button" disabled={page + 1 >= totalPages || loading} onClick={() => setPage((current) => Math.min(totalPages - 1, current + 1))}
              className="min-h-10 rounded-lg border border-slate-200 px-4 text-sm font-semibold text-slate-700 disabled:opacity-40">Next</button>
          </nav>
        )}
      </section>

      <div className="mt-8"><BroadcastCard /></div>
      {selected && modal === 'reschedule' && <RescheduleModal appointment={selected} onClose={() => setModal(null)} onRescheduled={() => void closeAndRefresh()} />}
      {selected && modal === 'consultation' && <ConsultationModal appointment={selected} onClose={() => setModal(null)} onCompleted={() => void closeAndRefresh()} />}
      {mobileSelected && (
        <MobileAppointmentDrawer
          appointment={mobileSelected}
          activeTab={appointmentTab}
          updating={updatingId === mobileSelected.id}
          onClose={() => setMobileSelected(null)}
          onConfirm={() => void confirmAppointment(mobileSelected)}
          onReschedule={() => openModal(mobileSelected, 'reschedule')}
          onConsult={() => openModal(mobileSelected, 'consultation')}
          onDelete={() => void removeAppointment(mobileSelected)}
        />
      )}
    </main>
  );
}

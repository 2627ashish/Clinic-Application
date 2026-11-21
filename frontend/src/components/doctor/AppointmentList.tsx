import { Check, MessageCircle, Phone, Trash2, Video, XCircle } from 'lucide-react';
import type { AppointmentStatus, AppointmentView } from '../../types';
import type { ConsultationType } from '../../types/appointment';
import { clinicDetails } from '../../utils/clinic';

interface AppointmentListProps {
  appointments: AppointmentView[];
  consultationType: ConsultationType;
  updatingId: string;
  onConfirm: (appointment: AppointmentView) => void;
  onUnavailable: (appointment: AppointmentView) => void;
  onReschedule: (appointment: AppointmentView) => void;
  onConsult: (appointment: AppointmentView) => void;
  onDelete: (appointment: AppointmentView) => void;
  onSelectMobile: (appointment: AppointmentView) => void;
}

const statusStyles: Record<AppointmentStatus, string> = {
  PENDING_DOCTOR_APPROVAL: 'bg-amber-50 text-amber-700',
  CONFIRMED: 'bg-emerald-50 text-emerald-700',
  RESCHEDULED: 'bg-sky-50 text-sky-700',
  COMPLETED: 'bg-slate-100 text-slate-600',
  CANCELLED: 'bg-rose-50 text-rose-700',
};

function formatTime(value: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

function whatsappCallUrl(phoneNumber: string, patientName: string): string | null {
  const phone = phoneNumber.replace(/\D/g, '');
  if (!phone) return null;
  const message = `Hello ${patientName}, Dr. B.S. Chauhan is ready for your scheduled online consultation.`;
  return `https://wa.me/${phone}?text=${encodeURIComponent(message)}`;
}

export default function AppointmentList({
  appointments,
  consultationType,
  updatingId,
  onConfirm,
  onUnavailable,
  onReschedule,
  onConsult,
  onDelete,
  onSelectMobile,
}: AppointmentListProps) {
  return (
    <>
      <div className="hidden overflow-x-auto md:block">
        <table className="hidden w-full min-w-[760px] text-left md:table">
          <thead className="bg-clinic-paper/70 text-xs uppercase tracking-wide text-slate-500">
            <tr><th className="px-6 py-3 font-semibold">Patient</th><th className="px-6 py-3 font-semibold">Visit</th><th className="px-6 py-3 font-semibold">Status</th><th className="px-6 py-3 text-right font-semibold">Actions</th></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {appointments.map((appointment) => (
              <tr key={appointment.id} className="align-top hover:bg-clinic-paper/40">
                <td className="px-6 py-4">
                  <p className="font-semibold text-clinic-ink">{appointment.patientName}</p>
                  <p className="mt-1 text-xs text-slate-500">{appointment.phoneNumber}</p>
                  <p className="mt-2 max-w-xs text-sm text-slate-600">{appointment.chiefComplaintSummary}</p>
                  {((appointment.medicalHistory?.length ?? 0) > 0 || appointment.isPregnant || appointment.ongoingTreatments) && (
                    <p className="mt-2 max-w-xs text-xs font-bold text-red-700">
                      MEDICAL ALERT: {[...(appointment.medicalHistory ?? []), ...(appointment.isPregnant ? ['Pregnant / breastfeeding'] : []), ...(appointment.ongoingTreatments ? [`Treatment: ${appointment.ongoingTreatments}`] : [])].join(' · ')}
                    </p>
                  )}
                </td>
                <td className="whitespace-nowrap px-6 py-4 text-sm text-slate-600">
                  <p>{formatTime(appointment.appointmentTime)}</p>
                  {consultationType === 'ONLINE'
                    ? <span className="mt-1 inline-flex rounded-full bg-indigo-50 px-2 py-1 text-[10px] font-bold uppercase tracking-wide text-indigo-700">Online consultation</span>
                    : <p className="mt-1 text-xs text-slate-500">{clinicDetails[appointment.clinicLocation].label}</p>}
                </td>
                <td className="px-6 py-4"><span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ${statusStyles[appointment.status]}`}>{appointment.status.replaceAll('_', ' ').toLowerCase()}</span></td>
                <td className="px-6 py-4">
                  <div className="flex justify-end gap-2">
                    {appointment.status === 'PENDING_DOCTOR_APPROVAL' && <button disabled={updatingId === appointment.id} onClick={() => onConfirm(appointment)} className="inline-flex items-center gap-1.5 rounded-lg bg-clinic-green px-3 py-2 text-xs font-semibold text-white hover:bg-clinic-ink disabled:opacity-50"><Check size={14} />{consultationType === 'ONLINE' ? 'Approve & Schedule' : 'Confirm'}</button>}
                    {consultationType === 'ONLINE' && appointment.status !== 'PENDING_DOCTOR_APPROVAL' && whatsappCallUrl(appointment.phoneNumber, appointment.patientName) && (
                      <a href={whatsappCallUrl(appointment.phoneNumber, appointment.patientName) ?? undefined} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1.5 rounded-lg bg-emerald-600 px-3 py-2 text-xs font-semibold text-white hover:bg-emerald-700"><MessageCircle size={14} /> Start WhatsApp Call</a>
                    )}
                    {consultationType === 'ONLINE' && appointment.meetingLink && <a href={appointment.meetingLink} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1.5 rounded-lg border border-indigo-200 px-3 py-2 text-xs font-semibold text-indigo-700 hover:bg-indigo-50"><Video size={14} /> Open Video Room</a>}
                    <button disabled={updatingId === appointment.id} onClick={() => onUnavailable(appointment)} className="inline-flex items-center gap-1.5 rounded-lg border border-rose-200 px-3 py-2 text-xs font-semibold text-rose-700 hover:bg-rose-50 disabled:opacity-50"><XCircle size={14} /> Unavailable</button>
                    {consultationType === 'IN_CLINIC' && <button onClick={() => onReschedule(appointment)} className="rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold text-clinic-ink hover:bg-slate-50">Reschedule</button>}
                    <button onClick={() => onConsult(appointment)} className="rounded-lg border border-clinic-green/20 bg-clinic-mint px-3 py-2 text-xs font-semibold text-clinic-green hover:bg-emerald-100">{consultationType === 'ONLINE' ? 'Write Rx & Bill' : 'Consult & bill'}</button>
                    <button type="button" title="Delete appointment" aria-label={`Delete ${appointment.patientName}'s appointment`}
                      disabled={updatingId === appointment.id} onClick={() => onDelete(appointment)}
                      className="grid h-9 w-9 place-items-center rounded-lg border border-rose-100 hover:bg-rose-50 disabled:opacity-50">
                      <Trash2 className="w-4 h-4 text-rose-500" />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="space-y-3 p-4 md:hidden">
        {appointments.map((appointment) => (
          <article key={appointment.id} role="button" tabIndex={0}
            onClick={() => onSelectMobile(appointment)}
            onKeyDown={(event) => {
              if (event.key === 'Enter' || event.key === ' ') onSelectMobile(appointment);
            }}
            className="cursor-pointer rounded-2xl border border-slate-100 bg-white p-4 shadow-sm">
            <div className="flex items-start justify-between gap-3">
              <div className="min-w-0">
                <p className="truncate font-bold text-clinic-ink">{appointment.patientName}</p>
                <a href={`tel:${appointment.phoneNumber}`} onClick={(event) => event.stopPropagation()}
                  className="mt-1 inline-flex items-center gap-1.5 text-sm text-slate-600">
                  <Phone size={14} className="text-clinic-green" />{appointment.phoneNumber}
                </a>
              </div>
              <span className={`shrink-0 rounded-full px-2.5 py-1 text-[11px] font-bold ${
                consultationType === 'ONLINE' ? 'bg-indigo-50 text-indigo-700' : statusStyles[appointment.status]
              }`}>{consultationType === 'ONLINE' ? 'Online' : appointment.status === 'PENDING_DOCTOR_APPROVAL' ? 'Pending' : appointment.status.charAt(0) + appointment.status.slice(1).toLowerCase()}</span>
            </div>
            <p className="mt-3 text-sm text-slate-500">{formatTime(appointment.appointmentTime)}</p>
          </article>
        ))}
      </div>
    </>
  );
}

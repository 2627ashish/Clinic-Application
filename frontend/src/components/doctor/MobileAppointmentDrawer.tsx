import { Phone, Trash2 } from 'lucide-react';
import type { AppointmentView } from '../../types';
import type { ConsultationType } from '../../types/appointment';
import { clinicDetails } from '../../utils/clinic';

interface MobileAppointmentDrawerProps {
  appointment: AppointmentView;
  activeTab: ConsultationType | 'WALK_IN';
  updating: boolean;
  onClose: () => void;
  onConfirm: () => void;
  onReschedule: () => void;
  onConsult: () => void;
  onDelete: () => void;
}

function formatTime(value: string): string {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value));
}

export default function MobileAppointmentDrawer({
  appointment,
  activeTab,
  updating,
  onClose,
  onConfirm,
  onReschedule,
  onConsult,
  onDelete,
}: MobileAppointmentDrawerProps) {
  return (
    <div className="fixed inset-0 z-40 flex items-end bg-slate-900/60 md:hidden" role="presentation" onClick={onClose}>
      <section role="dialog" aria-modal="true" aria-labelledby="mobile-appointment-title"
        onClick={(event) => event.stopPropagation()}
        className="max-h-[88vh] w-full overflow-y-auto rounded-t-3xl bg-white p-5 pb-8 shadow-2xl">
        <div className="mx-auto mb-4 h-1.5 w-12 rounded-full bg-slate-200" />
        <div className="flex items-start justify-between gap-3">
          <div>
            <h2 id="mobile-appointment-title" className="text-xl font-bold text-clinic-ink">{appointment.patientName}</h2>
            <a href={`tel:${appointment.phoneNumber}`} className="mt-1 inline-flex items-center gap-2 text-sm text-clinic-green"><Phone size={15} />{appointment.phoneNumber}</a>
          </div>
          <button type="button" onClick={onClose} aria-label="Close appointment details" className="rounded-full px-3 py-1 text-slate-500 hover:bg-slate-100">Close</button>
        </div>
        <div className="mt-4 space-y-2 rounded-2xl bg-clinic-paper p-4 text-sm text-slate-700">
          <p><strong>Visit:</strong> {formatTime(appointment.appointmentTime)}</p>
          <p><strong>Type:</strong> {appointment.consultationType === 'ONLINE' ? 'Online consultation' : clinicDetails[appointment.clinicLocation].label}</p>
          <p><strong>Status:</strong> {appointment.status.replaceAll('_', ' ').toLowerCase()}</p>
          <p><strong>Age / Gender:</strong> {appointment.patientAge ?? 'Not recorded'} / {appointment.gender ?? 'Not recorded'}</p>
          {appointment.dateOfBirth && <p><strong>Date of birth:</strong> {appointment.dateOfBirth}</p>}
          <p><strong>Reason:</strong> {appointment.chiefComplaintSummary || 'Not provided'}</p>
          {appointment.doctorNotes && <p><strong>Notes:</strong> {appointment.doctorNotes}</p>}
          {appointment.medicalHistory?.length ? <p><strong>Medical history:</strong> {appointment.medicalHistory.join(', ')}</p> : null}
          {appointment.ongoingTreatments && <p><strong>Medication / treatment:</strong> {appointment.ongoingTreatments}</p>}
          {appointment.medicalDeclaration && (
            <div className="border-t border-slate-200 pt-2">
              <p className="font-bold text-clinic-ink">Medical declaration</p>
              <p>Chronic illness: {appointment.medicalDeclaration.hasChronicIllness ? 'Yes' : 'No'} · Long-term medication: {appointment.medicalDeclaration.takingLongTermMedication ? 'Yes' : 'No'}</p>
              <p>Previous dental treatment: {appointment.medicalDeclaration.hadPreviousDentalTreatment ? 'Yes' : 'No'} · RCT commitment: {appointment.medicalDeclaration.rctCommitment ? 'Yes' : 'No'}</p>
              {appointment.medicalDeclaration.illnessDetails && <p>Illness details: {appointment.medicalDeclaration.illnessDetails}</p>}
              {appointment.medicalDeclaration.medicationDetails && <p>Medication details: {appointment.medicalDeclaration.medicationDetails}</p>}
              {appointment.medicalDeclaration.isPregnant !== null && <p>Pregnant: {appointment.medicalDeclaration.isPregnant ? 'Yes' : 'No'}</p>}
              {appointment.medicalDeclaration.isBreastfeeding !== null && <p>Breastfeeding: {appointment.medicalDeclaration.isBreastfeeding ? 'Yes' : 'No'}</p>}
            </div>
          )}
        </div>
        <div className="mt-4 grid grid-cols-2 gap-2">
          <a href={`tel:${appointment.phoneNumber}`} className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-slate-200 font-semibold text-clinic-ink"><Phone size={16} />Call</a>
          {appointment.status === 'PENDING_DOCTOR_APPROVAL' && <button type="button" onClick={onConfirm} className="min-h-11 rounded-xl bg-clinic-green font-semibold text-white">Confirm</button>}
          {activeTab === 'IN_CLINIC' && <button type="button" onClick={onReschedule} className="min-h-11 rounded-xl border border-slate-200 font-semibold text-clinic-ink">Reschedule</button>}
          <button type="button" onClick={onConsult} className="min-h-11 rounded-xl bg-clinic-mint font-semibold text-clinic-green">Consult &amp; Bill</button>
          <button type="button" disabled={updating} onClick={onDelete}
            className="col-span-2 inline-flex min-h-11 items-center justify-center gap-2 rounded-xl border border-rose-200 font-semibold text-rose-600 disabled:opacity-50"><Trash2 className="w-4 h-4 text-rose-500" />Delete</button>
        </div>
      </section>
    </div>
  );
}

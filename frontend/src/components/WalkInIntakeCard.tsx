import { useState } from 'react';
import type { FormEvent } from 'react';
import { Phone, UserRound } from 'lucide-react';
import type { AppointmentView, WalkInRequest } from '../types';
import { createWalkIn } from '../api/client';
import { clinicDetails, clinicLocations, normalizeIndianPhone } from '../utils/clinic';
import type { ClinicLocation } from '../utils/clinic';
import { calculateAge } from '../utils/demographics';

interface WalkInIntakeCardProps {
  appointments: AppointmentView[];
  onStartConsultation: (appointmentId: string) => void;
}

export default function WalkInIntakeCard({ appointments, onStartConsultation }: WalkInIntakeCardProps) {
  const [patientName, setPatientName] = useState('');
  const [phone, setPhone] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [gender, setGender] = useState<'Male' | 'Female' | 'Other'>('Female');
  const [clinicLocation, setClinicLocation] = useState<ClinicLocation>('SEC_44');
  const [source, setSource] = useState<WalkInRequest['source']>('DIRECT_WALK_IN');
  const [quotedFee, setQuotedFee] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);
  const age = calculateAge(dateOfBirth);

  const todayWalkIns = appointments.filter((appointment) =>
    appointment.intakeSource
    && new Date(appointment.appointmentTime).toDateString() === new Date().toDateString(),
  );

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const normalizedPhone = normalizeIndianPhone(phone);
    if (!normalizedPhone) {
      setError('Enter a valid Indian mobile number.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      const request: WalkInRequest = {
        patientName: patientName.trim(),
        phoneNumber: normalizedPhone,
        dateOfBirth,
        gender,
        clinicLocation,
        source,
        quotedFee: quotedFee ? Number(quotedFee) : null,
        notes: notes.trim(),
      };
      const created = await createWalkIn(request);
      setPatientName('');
      setPhone('');
      setDateOfBirth('');
      setQuotedFee('');
      setNotes('');
      onStartConsultation(created.id);
    } catch {
      setError('The walk-in record could not be created. Check the phone number and try again.');
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="grid gap-6 p-5 sm:p-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)]">
      <form onSubmit={(event) => void submit(event)} className="space-y-4 rounded-2xl bg-clinic-paper p-5">
        <div>
          <h3 className="text-lg font-bold text-clinic-ink">Quick patient intake</h3>
          <p className="mt-1 text-sm text-slate-500">For direct clinic arrivals and phone enquiries.</p>
        </div>
        <label className="block text-sm font-semibold text-clinic-ink">Patient Full Name
          <input required maxLength={100} value={patientName} onChange={(event) => setPatientName(event.target.value)} className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3 font-normal" />
        </label>
        <label className="block text-sm font-semibold text-clinic-ink">Phone Number
          <input required type="tel" inputMode="tel" value={phone} onChange={(event) => setPhone(event.target.value)} className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3 font-normal" />
        </label>
        <div className="grid grid-cols-2 gap-3">
          <label className="text-sm font-semibold text-clinic-ink">Date of birth
            <input required type="date" max={new Date().toISOString().slice(0, 10)} value={dateOfBirth} onChange={(event) => setDateOfBirth(event.target.value)} className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3 font-normal" />
            <span className="mt-1 block text-xs font-normal text-slate-500">Live age: {age === null ? '—' : `${age} years`}</span>
          </label>
          <label className="text-sm font-semibold text-clinic-ink">Gender
            <select value={gender} onChange={(event) => setGender(event.target.value as WalkInRequest['gender'])} className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3 font-normal">
              <option>Female</option><option>Male</option><option>Other</option>
            </select>
          </label>
        </div>
        <div className="grid grid-cols-2 gap-3">
          <label className="text-sm font-semibold text-clinic-ink">Clinic Branch
            <select value={clinicLocation} onChange={(event) => setClinicLocation(event.target.value as ClinicLocation)} className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3 font-normal">
              {clinicLocations.map((location) => <option key={location} value={location}>{clinicDetails[location].label}</option>)}
            </select>
          </label>
          <label className="text-sm font-semibold text-clinic-ink">Source
            <select value={source} onChange={(event) => setSource(event.target.value as WalkInRequest['source'])} className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3 font-normal">
              <option value="DIRECT_WALK_IN">Direct Walk-In</option><option value="PHONE_CALL">Phone Call</option>
            </select>
          </label>
        </div>
        <label className="block text-sm font-semibold text-clinic-ink">Quoted Fee (₹)
          <input type="number" min="0" step="1" value={quotedFee} onChange={(event) => setQuotedFee(event.target.value)} className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 bg-white px-3 font-normal" />
        </label>
        <label className="block text-sm font-semibold text-clinic-ink">Notes
          <textarea maxLength={1000} rows={2} value={notes} onChange={(event) => setNotes(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 font-normal" />
        </label>
        {error && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-700">{error}</p>}
        <button disabled={saving} className="min-h-12 w-full rounded-xl bg-clinic-green px-4 font-semibold text-white disabled:opacity-60">
          {saving ? 'Creating record…' : 'Create Instant Walk-In Record'}
        </button>
      </form>

      <section>
        <h3 className="text-lg font-bold text-clinic-ink">Today’s Clinic Walk-Ins</h3>
        <p className="mt-1 text-sm text-slate-500">{todayWalkIns.length} records today</p>
        {todayWalkIns.length === 0 ? (
          <div className="mt-4 rounded-2xl border border-dashed border-slate-300 p-8 text-center text-sm text-slate-500">New phone and walk-in records will appear here.</div>
        ) : (
          <div className="mt-4 space-y-3">
            {todayWalkIns.map((appointment) => (
              <article key={appointment.id} className="flex flex-col justify-between gap-3 rounded-2xl border border-slate-100 p-4 sm:flex-row sm:items-center">
                <div>
                  <p className="flex items-center gap-2 font-semibold text-clinic-ink"><UserRound size={16} />{appointment.patientName}</p>
                  <p className="mt-1 flex items-center gap-2 text-sm text-slate-500"><Phone size={14} />{appointment.phoneNumber}</p>
                  <p className="mt-1 text-xs text-slate-500">{appointment.intakeSource === 'PHONE_CALL' ? 'Phone Call' : 'Direct Walk-In'} · {clinicDetails[appointment.clinicLocation].label}{appointment.quotedFee != null ? ` · ₹${appointment.quotedFee}` : ''}</p>
                  {((appointment.medicalHistory?.length ?? 0) > 0 || appointment.isPregnant || appointment.ongoingTreatments) && (
                    <p className="mt-2 text-xs font-bold text-red-700">MEDICAL ALERT: {[...(appointment.medicalHistory ?? []), ...(appointment.isPregnant ? ['Pregnant / breastfeeding'] : []), ...(appointment.ongoingTreatments ? [`Treatment: ${appointment.ongoingTreatments}`] : [])].join(' · ')}</p>
                  )}
                </div>
                <button type="button" onClick={() => onStartConsultation(appointment.id)} className="min-h-10 rounded-lg bg-clinic-mint px-3 py-2 text-sm font-semibold text-clinic-green hover:bg-emerald-100">
                  Start Consultation &amp; Rx
                </button>
              </article>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}

import { useState } from 'react';
import type { FormEvent } from 'react';
import { Check, X } from 'lucide-react';
import { submitAiIntake } from '../../api/appointmentApi';
import type { AppointmentResponse, ConsultationType } from '../../types/appointment';
import { clinicDetails, clinicLocations, isClinicLocation, normalizeIndianPhone } from '../../utils/clinic';
import type { ClinicLocation } from '../../utils/clinic';
import WheelSlotPicker from '../intake/WheelSlotPicker';

interface QuickBookingModalProps {
  onClose: () => void;
  initialSymptoms?: string;
  initialAge?: number;
  initialGender?: 'Male' | 'Female' | 'Other' | '';
  initialConsultationType?: ConsultationType;
}

export default function QuickBookingModal({
  onClose,
  initialSymptoms = '',
  initialAge,
  initialGender = '',
  initialConsultationType = 'IN_CLINIC',
}: QuickBookingModalProps) {
  const [preferredTime, setPreferredTime] = useState('');
  const [clinicLocation, setClinicLocation] = useState<ClinicLocation>('SEC_44');
  const [consultationType, setConsultationType] = useState<ConsultationType>(initialConsultationType);
  const [patientName, setPatientName] = useState('');
  const [age, setAge] = useState(initialAge ? String(initialAge) : '');
  const [gender, setGender] = useState<'Male' | 'Female' | 'Other' | ''>(initialGender);
  const [phoneNumber, setPhoneNumber] = useState('');
  const [symptomsDescription, setSymptomsDescription] = useState(initialSymptoms);
  const [whatsappMarketingOptIn, setWhatsappMarketingOptIn] = useState(false);
  const [appointment, setAppointment] = useState<AppointmentResponse | null>(null);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function submitBooking(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    const normalizedPhone = normalizeIndianPhone(phoneNumber);
    if (!normalizedPhone) {
      setError('Enter a valid Indian mobile number (10 digits, optionally prefixed with 0 or +91).');
      return;
    }
    if (!preferredTime) {
      setError('Choose an available appointment time to continue.');
      return;
    }
    if (!age || Number(age) < 1 || Number(age) > 120 || !gender) {
      setError('Enter an age from 1 to 120 and choose a gender.');
      return;
    }

    setSubmitting(true);
    try {
      const result = await submitAiIntake({
        patientName: patientName.trim(),
        phoneNumber: normalizedPhone,
        age: Number(age),
        gender,
        preferredTime,
        clinicLocation,
        symptomsDescription: symptomsDescription.trim(),
        consultationType,
        whatsappMarketingOptIn,
      });
      setAppointment(result);
    } catch {
      setError('We could not submit your request. Please check your connection and try again.');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-clinic-ink/60 p-3 backdrop-blur-sm sm:p-6" role="presentation">
      <section role="dialog" aria-modal="true" aria-labelledby="quick-booking-title" className="max-h-[92vh] w-full max-w-xl overflow-y-auto rounded-3xl bg-white p-5 shadow-2xl sm:p-7">
        <div className="flex items-start justify-between gap-4">
          <div>
            <p className="text-sm font-bold uppercase tracking-wider text-clinic-green">Chauhan Dental Clinic</p>
            <h2 id="quick-booking-title" className="mt-1 text-2xl font-bold text-clinic-ink">
              {appointment ? 'Request received' : 'Need Quick Appointment?'}
            </h2>
          </div>
          <button type="button" onClick={onClose} aria-label="Close booking" className="rounded-full p-2 text-slate-500 hover:bg-slate-100">
            <X size={20} />
          </button>
        </div>

        {appointment ? (
          <div className="py-10 text-center">
            <span className="mx-auto grid h-16 w-16 place-items-center rounded-full bg-emerald-100 text-emerald-700"><Check size={34} /></span>
            <h3 className="mt-5 text-xl font-bold text-clinic-ink">Appointment requested!</h3>
            <p className="mt-2 text-sm leading-6 text-slate-600">
              Your {appointment.consultationType === 'ONLINE' ? 'online consultation' : 'in-clinic visit'} request for {new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' })
                .format(new Date(appointment.appointmentTime))}{appointment.consultationType === 'IN_CLINIC' ? ` at ${clinicDetails[appointment.clinicLocation].address}` : ''} is received.
              Dr. Chauhan’s team will review and follow up via WhatsApp at {appointment.phoneNumber}.
            </p>
            <button type="button" onClick={onClose} className="mt-6 rounded-xl bg-clinic-green px-5 py-3 font-semibold text-white">Done</button>
          </div>
        ) : (
          <form onSubmit={submitBooking} className="mt-5 space-y-5">
            <fieldset>
              <legend className="text-sm font-semibold text-clinic-ink">Choose consultation mode</legend>
              <div className="mt-2 grid gap-2 sm:grid-cols-2">
                {([
                  ['IN_CLINIC', '🏥 In-Clinic Visit'],
                  ['ONLINE', '💻 Online Video Consultation'],
                ] as const).map(([type, label]) => (
                  <button key={type} type="button" aria-pressed={consultationType === type}
                    onClick={() => setConsultationType(type)}
                    className={`min-h-12 rounded-xl border px-3 py-2 text-sm font-semibold ${consultationType === type ? 'border-clinic-green bg-clinic-mint text-clinic-green' : 'border-slate-200 text-slate-600'}`}>
                    {label}{type === 'ONLINE' ? ' (WhatsApp)' : ''}
                  </button>
                ))}
              </div>
              {consultationType === 'ONLINE' && <p className="mt-2 text-xs leading-5 text-slate-600">Dr. B.S. Chauhan will connect with you via WhatsApp Video Call at your booked time.</p>}
            </fieldset>
            {consultationType === 'IN_CLINIC' && <label className="block text-sm font-semibold text-clinic-ink">
              Choose a clinic
              <select
                value={clinicLocation}
                onChange={(event) => {
                  if (!isClinicLocation(event.target.value)) return;
                  setClinicLocation(event.target.value);
                  setPreferredTime('');
                }}
                className="mt-1.5 w-full rounded-xl border border-slate-200 px-3 py-2.5 font-normal"
              >
                {clinicLocations.map((location) => {
                  const clinic = clinicDetails[location];
                  return (
                    <option key={location} value={location}>{clinic.label} · {clinic.address}</option>
                  );
                })}
              </select>
              <span className="mt-1 block text-xs font-normal text-slate-500">{clinicDetails[clinicLocation].hours}</span>
            </label>}
            <WheelSlotPicker value={preferredTime} onChange={setPreferredTime} clinicLocation={clinicLocation} />
            <div className="grid gap-4 sm:grid-cols-2">
              <label className="text-sm font-semibold text-clinic-ink">
                Full name
                <input required maxLength={100} autoComplete="name" value={patientName} onChange={(event) => setPatientName(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-200 px-3 py-2.5 font-normal outline-none focus:border-clinic-green focus:ring-2 focus:ring-clinic-green/15" />
              </label>
              <label className="text-sm font-semibold text-clinic-ink">
                Age
                <input required min="1" max="120" type="number" inputMode="numeric" value={age}
                  onChange={(event) => setAge(event.target.value)}
                  className="mt-1.5 w-full rounded-xl border border-slate-200 px-3 py-2.5 font-normal outline-none focus:border-clinic-green focus:ring-2 focus:ring-clinic-green/15" />
              </label>
              <label className="text-sm font-semibold text-clinic-ink">
                WhatsApp number
                <input required type="tel" inputMode="tel" autoComplete="tel" value={phoneNumber} onChange={(event) => setPhoneNumber(event.target.value)} aria-describedby="mobile-hint" className="mt-1.5 w-full rounded-xl border border-slate-200 px-3 py-2.5 font-normal outline-none focus:border-clinic-green focus:ring-2 focus:ring-clinic-green/15" />
                <span id="mobile-hint" className="mt-1 block text-xs font-normal text-slate-500">10 digits, or include 0 / +91.</span>
              </label>
              <fieldset className="text-sm font-semibold text-clinic-ink">
                <legend>Gender</legend>
                <div className="mt-1.5 flex gap-2">
                  {(['Male', 'Female', 'Other'] as const).map((value) => (
                    <button key={value} type="button" aria-pressed={gender === value}
                      onClick={() => setGender(value)}
                      className={`min-h-11 flex-1 rounded-full border px-3 text-sm font-semibold ${gender === value ? 'border-clinic-green bg-clinic-mint text-clinic-green' : 'border-slate-200 text-slate-600'}`}>
                      {value}
                    </button>
                  ))}
                </div>
              </fieldset>
            </div>
            <label className="block text-sm font-semibold text-clinic-ink">
              Quick reason for visit
              <textarea required maxLength={1000} rows={3} value={symptomsDescription} onChange={(event) => setSymptomsDescription(event.target.value)} className="mt-1.5 w-full rounded-xl border border-slate-200 px-3 py-2.5 font-normal outline-none focus:border-clinic-green focus:ring-2 focus:ring-clinic-green/15" />
            </label>
            <label className="flex items-start gap-2 text-sm leading-5 text-slate-600">
              <input type="checkbox" checked={whatsappMarketingOptIn} onChange={(event) => setWhatsappMarketingOptIn(event.target.checked)} className="mt-1 accent-clinic-green" />
              <span>I agree to receive occasional clinic news and greetings on WhatsApp. Reply STOP any time to opt out.</span>
            </label>
            {error && <p role="alert" className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{error}</p>}
            <button type="submit" disabled={submitting} className="w-full rounded-xl bg-clinic-green px-5 py-3 font-semibold text-white transition hover:bg-clinic-ink disabled:cursor-wait disabled:opacity-60">
              {submitting ? 'Submitting request…' : 'Request appointment with Dr. Chauhan'}
            </button>
            <p className="text-center text-xs text-slate-500">Your slot is provisional until the clinic team confirms it.</p>
          </form>
        )}
      </section>
    </div>
  );
}

import { MessageCircle, Phone, Zap } from 'lucide-react';

interface MobileStickyBarProps {
  onBookAppointment: () => void;
}

const clinicPhone = import.meta.env.VITE_CLINIC_PHONE_PRIMARY || '+91-98107-64715';
const clinicPhoneDigits = clinicPhone.replace(/\D/g, '');
const whatsappNumber = clinicPhoneDigits.startsWith('91') ? clinicPhoneDigits : `91${clinicPhoneDigits}`;
const whatsappUrl = `https://wa.me/${whatsappNumber}?text=${encodeURIComponent(
  'Hi Dr. Chauhan, I would like to book a dental appointment.',
)}`;

export default function MobileStickyBar({ onBookAppointment }: MobileStickyBarProps) {
  return (
    <nav aria-label="Quick contact and booking" className="fixed inset-x-0 bottom-0 z-40 border-t border-slate-200 bg-white/95 px-3 pt-2 shadow-[0_-8px_24px_rgba(15,23,42,0.08)] backdrop-blur md:hidden [padding-bottom:env(safe-area-inset-bottom)]">
      <div className="mx-auto flex max-w-lg items-center gap-2">
        <a href={`tel:${clinicPhoneDigits}`} className="flex min-h-12 flex-1 flex-col items-center justify-center rounded-xl px-1 text-[11px] font-semibold leading-4 text-slate-700" aria-label={`Call ${clinicPhone}`}>
          <span className="flex items-center gap-1"><Phone size={15} /> Call</span>
          <span>{clinicPhone}</span>
        </a>
        <button type="button" onClick={onBookAppointment} className="inline-flex min-h-12 flex-[1.25] items-center justify-center gap-2 rounded-xl bg-clinic-green px-3 text-sm font-bold text-white shadow-md shadow-clinic-green/20">
          <Zap size={17} /> Book Appointment
        </button>
        <a href={whatsappUrl} target="_blank" rel="noreferrer" aria-label="Message Chauhan Dental Clinic on WhatsApp" className="grid h-12 w-12 shrink-0 place-items-center rounded-xl bg-emerald-50 text-emerald-700">
          <MessageCircle size={20} />
        </a>
      </div>
    </nav>
  );
}

import { useState } from 'react';
import { MessageCircle } from 'lucide-react';
import QuickBookingModal from '../components/booking/QuickBookingModal';
import AiSelfHelpReceptionistModal from '../components/intake/AiSelfHelpReceptionistModal';
import ClinicContactBar from '../components/landing/ClinicContactBar';
import HeroSection from '../components/landing/HeroSection';
import ServicesSection from '../components/landing/ServicesSection';
import type { ClinicPage } from '../components/Navbar';
import type { ConsultationType } from '../types/appointment';

interface PatientLandingPageProps {
  onNavigate: (page: ClinicPage) => void;
}

export default function PatientLandingPage({ onNavigate }: PatientLandingPageProps) {
  const [showSelfHelp, setShowSelfHelp] = useState(false);
  const [showQuickBooking, setShowQuickBooking] = useState(false);
  const [bookingSymptoms, setBookingSymptoms] = useState('');
  const [bookingAge, setBookingAge] = useState<number>();
  const [bookingGender, setBookingGender] = useState<'Male' | 'Female' | 'Other' | ''>('');
  const [bookingConsultationType, setBookingConsultationType] = useState<ConsultationType>('IN_CLINIC');

  function escalateToBooking(symptoms: string, age: number, gender: 'Male' | 'Female' | 'Other', consultationType: ConsultationType) {
    setBookingSymptoms(symptoms);
    setBookingAge(age);
    setBookingGender(gender);
    setBookingConsultationType(consultationType);
    setShowSelfHelp(false);
    setShowQuickBooking(true);
  }

  return (
    <main>
      <HeroSection
        onQuickBooking={() => {
          setBookingSymptoms('');
          setShowQuickBooking(true);
        }}
      />
      <ServicesSection />

      <ClinicContactBar />

      <section className="mx-auto flex max-w-7xl flex-col items-start justify-between gap-5 px-5 py-8 sm:flex-row sm:items-center sm:px-8 sm:py-12">
        <div>
          <p className="text-sm font-bold uppercase tracking-[0.16em] text-clinic-green">We’re here to help</p>
          <h2 className="mt-2 text-2xl font-bold text-clinic-ink">Not sure where to start?</h2>
          <p className="mt-1 text-sm text-slate-500">Tell our virtual receptionist what is on your mind.</p>
        </div>
        <button type="button" onClick={() => setShowSelfHelp(true)} className="rounded-xl bg-clinic-ink px-5 py-3 font-semibold text-white transition hover:bg-clinic-green">
          Talk to AI Receptionist (Maya)
        </button>
      </section>

      <footer className="flex flex-col items-center justify-between gap-3 border-t border-clinic-ink/5 bg-white px-5 py-6 text-xs text-slate-500 sm:flex-row sm:px-8">
        <span>© {new Date().getFullYear()} Chauhan Dental Clinic · Care that makes you smile</span>
        <button type="button" onClick={() => onNavigate('doctor')} className="font-semibold text-clinic-green hover:text-clinic-ink">Doctor portal</button>
      </footer>

      <button
        type="button"
        onClick={() => {
          setShowSelfHelp(true);
        }}
        aria-label="Talk to AI Receptionist Maya"
        className="fixed bottom-5 right-5 z-30 inline-flex min-h-12 items-center gap-2 rounded-full bg-clinic-green px-5 py-3.5 text-sm font-semibold text-white shadow-xl transition hover:-translate-y-0.5 hover:bg-clinic-ink"
      >
        <MessageCircle size={18} aria-hidden="true" />
        <span>Talk to AI Receptionist (Maya)</span>
      </button>

      {showSelfHelp && (
        <AiSelfHelpReceptionistModal
          onClose={() => setShowSelfHelp(false)}
          onBook={escalateToBooking}
        />
      )}
      {showQuickBooking && (
        <QuickBookingModal
          initialSymptoms={bookingSymptoms}
          initialAge={bookingAge}
          initialGender={bookingGender}
          initialConsultationType={bookingConsultationType}
          onClose={() => setShowQuickBooking(false)}
        />
      )}
    </main>
  );
}

import { ArrowRight, CalendarPlus, HeartPulse, MessageCircle, ShieldCheck, Sparkles, Star, MapPin, Phone, Clock3 } from 'lucide-react';

interface HeroSectionProps {
  onQuickBooking: () => void;
}

const trustBadges = [
  { icon: Star, label: '5.0 Star Google Rating · Verified Patient Reviews' },
  { icon: MapPin, label: 'Two clinics · Sector-44 & Sector-82, Noida' },
  { icon: Phone, label: '+91-98107-64715 / +91-97177-73179' },
  { icon: Clock3, label: 'Mon–Sat · Clinic-specific shift hours' },
];

const whatsappBookingUrl = 'https://wa.me/919810764715?text=Hi%20Dr.%20Chauhan,%20I%20want%20to%20book%20a%20dental%20appointment';

export default function HeroSection({ onQuickBooking }: HeroSectionProps) {
  return (
    <section className="mx-auto grid max-w-7xl gap-10 px-5 pb-16 pt-12 sm:px-8 lg:grid-cols-[1.05fr_0.95fr] lg:items-center lg:pb-24 lg:pt-20">
      <div>
        <span className="inline-flex items-center gap-2 rounded-full border border-clinic-green/10 bg-white px-3.5 py-2 text-sm font-semibold text-clinic-green shadow-sm">
          <ShieldCheck size={16} aria-hidden="true" /> Welcoming new patients
        </span>
        <div className="mt-6 max-w-2xl">
          <h1 className="text-4xl font-bold leading-[1.12] tracking-tight text-clinic-ink sm:text-5xl lg:text-6xl">
            Chauhan Dental Clinic
          </h1>
          <p className="mt-2 text-2xl font-semibold text-emerald-800 sm:text-3xl">
            Dr. B.S. Chauhan
          </p>
        </div>
        <p className="mt-5 max-w-xl text-lg leading-7 text-slate-600">
          Advanced, Painless Dental Care in Sector-44 & Sector-82, Noida
        </p>
        <div className="mt-7 flex flex-wrap gap-2" aria-label="Clinic highlights">
          {trustBadges.map(({ icon: Icon, label }) => (
            <span key={label} className="inline-flex items-center gap-2 rounded-full bg-white px-3 py-2 text-xs font-semibold text-clinic-ink shadow-sm sm:text-sm">
              <Icon size={15} className="text-clinic-green" aria-hidden="true" /> {label}
            </span>
          ))}
        </div>
        <button
          type="button"
          onClick={onQuickBooking}
          className="mt-8 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-clinic-green px-6 py-3.5 font-semibold text-white shadow-lg shadow-clinic-green/15 transition hover:-translate-y-0.5 hover:bg-clinic-ink sm:w-auto"
        >
          <CalendarPlus size={18} aria-hidden="true" />
          Book Appointment Online
          <ArrowRight size={17} aria-hidden="true" />
        </button>
        <a
          href={whatsappBookingUrl}
          target="_blank"
          rel="noreferrer"
          className="mt-3 inline-flex w-full items-center justify-center gap-2 rounded-xl border border-clinic-green/20 bg-white px-6 py-3.5 font-semibold text-clinic-green transition hover:-translate-y-0.5 hover:border-clinic-green sm:ml-3 sm:mt-8 sm:w-auto"
        >
          <MessageCircle size={18} aria-hidden="true" />
          Book via WhatsApp
        </a>
      </div>

      <div className="relative mx-auto w-full max-w-xl">
        <div className="absolute -left-5 top-8 h-28 w-28 rounded-full bg-emerald-100 blur-2xl" aria-hidden="true" />
        <div className="relative rounded-[2rem] bg-gradient-to-br from-[#dcece4] via-[#f2eee4] to-[#e6f0eb] p-5 sm:p-8">
          <div className="flex min-h-[310px] flex-col justify-between rounded-[1.5rem] bg-white/80 p-6 shadow-soft backdrop-blur sm:min-h-[360px] sm:p-8">
            <div className="flex items-center justify-between">
              <span className="grid h-12 w-12 place-items-center rounded-2xl bg-clinic-green text-white">
                <HeartPulse size={24} aria-hidden="true" />
              </span>
              <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-clinic-green">Care that puts you first</span>
            </div>
            <div className="py-8 text-center">
              <div className="mx-auto grid h-36 w-36 place-items-center rounded-full border-[14px] border-white bg-clinic-mint shadow-inner">
                <Sparkles size={56} strokeWidth={1.4} className="text-clinic-green" aria-hidden="true" />
              </div>
              <p className="mt-6 text-2xl font-bold text-clinic-ink">A comfortable visit starts here</p>
              <p className="mt-2 text-sm text-slate-500">Share what you need, then choose a time that works for you.</p>
            </div>
            <div className="rounded-2xl bg-clinic-paper px-4 py-3 text-sm text-slate-600">
              A team member reviews every appointment request before confirming.
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

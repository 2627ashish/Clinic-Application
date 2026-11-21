import { ArrowRight, Calendar, Clock3, ExternalLink, MapPin, MessageCircle, Star } from 'lucide-react';
import ClinicLogo from '../ClinicLogo';
import { DENTAL_IMAGES } from '../../constants/dentalImages';
import { justDialProfileUrl } from '../../utils/clinic';
import PostCareCarousel from './PostCareCarousel';

interface HeroSectionProps {
  onQuickBooking: () => void;
}

const whatsappBookingUrl = `https://wa.me/919810764715?text=${encodeURIComponent(
  'Hi Dr. Chauhan, I would like to book a dental appointment.',
)}`;

export default function HeroSection({ onQuickBooking }: HeroSectionProps) {
  return (
    <section className="relative overflow-hidden bg-[radial-gradient(ellipse_at_top_right,_rgba(16,185,129,0.10),_transparent_44%),linear-gradient(180deg,_#f8fbf9_0%,_#f3f8f5_100%)] pb-12 pt-9 sm:pb-16 sm:pt-14">
      <div className="mx-auto grid max-w-7xl items-center gap-10 px-5 sm:px-8 lg:grid-cols-[1.03fr_0.97fr] lg:gap-14">
        <div className="relative z-10">
          <span className="inline-flex items-center gap-2 rounded-full border border-emerald-200/80 bg-white/80 px-4 py-2 text-sm font-semibold text-emerald-800 shadow-sm backdrop-blur">
            <span aria-hidden="true">✨</span> Advanced &amp; Painless Dental Care in Noida
          </span>
          <h1 className="mt-6 max-w-2xl text-5xl font-extrabold leading-[1.02] tracking-[-0.045em] text-slate-900 sm:text-6xl">
            Chauhan Dental Clinic
          </h1>
          <p className="mt-4 text-2xl font-bold tracking-tight text-emerald-700 sm:text-3xl">
            Dr. B.S. Chauhan
          </p>
          <p className="mt-1 text-sm font-semibold tracking-wide text-emerald-800/80">
            B.D.S. Noida <span className="mx-1 text-slate-300">|</span> Reg: A-5651
          </p>
          <p className="mt-5 max-w-xl text-base leading-7 text-slate-600 sm:text-lg sm:leading-8">
            Your smile, our commitment. Experience gentle, sterile, and advanced dentistry across Sector-44 &amp; Sector-82.
          </p>

          <div className="mt-7 flex flex-col gap-3 sm:flex-row">
            <button type="button" onClick={onQuickBooking}
              className="inline-flex min-h-[3.25rem] items-center justify-center gap-2 rounded-2xl bg-emerald-700 px-5 py-3.5 text-sm font-bold text-white shadow-xl shadow-emerald-900/15 transition hover:-translate-y-0.5 hover:bg-emerald-800 sm:text-base">
              <Calendar className="mr-2 h-5 w-5" aria-hidden="true" />
              Book Appointment Online
              <ArrowRight className="ml-2 h-4 w-4" aria-hidden="true" />
            </button>
            <a href={whatsappBookingUrl} target="_blank" rel="noreferrer"
              className="inline-flex min-h-[3.25rem] items-center justify-center gap-2 rounded-2xl border border-emerald-200 bg-white/80 px-5 py-3.5 text-sm font-bold text-emerald-800 shadow-sm backdrop-blur transition hover:-translate-y-0.5 hover:border-emerald-400 sm:text-base">
              <MessageCircle className="mr-2 h-5 w-5 text-emerald-600" aria-hidden="true" />
              WhatsApp Quick Connect
            </a>
          </div>

          <div className="mt-8 grid gap-2 sm:grid-cols-3" aria-label="Clinic highlights">
            <a href={justDialProfileUrl} target="_blank" rel="noreferrer"
              className="flex items-center gap-2 rounded-xl border border-amber-100 bg-white/80 px-3 py-2.5 text-left shadow-sm backdrop-blur sm:col-span-2">
              <Star size={18} className="shrink-0 text-amber-500" fill="currentColor" aria-hidden="true" />
              <span className="min-w-0 flex-1">
                <strong className="block text-sm text-slate-900">4.8 / 5.0 Rating</strong>
                <span className="block text-xs leading-4 text-slate-600">50+ Verified Patient Reviews on JustDial</span>
              </span>
              <ExternalLink className="h-3.5 w-3.5 shrink-0 text-amber-500" aria-hidden="true" />
            </a>
            <div className="flex items-center gap-2 rounded-xl border border-white/80 bg-white/70 px-3 py-2.5 text-xs font-semibold text-slate-700 shadow-sm backdrop-blur sm:text-sm">
              <MapPin size={16} className="shrink-0 text-emerald-700" aria-hidden="true" />
              <span>Near Chauhan Stationary, Sector 44, Noida</span>
            </div>
            <div className="flex items-center gap-2 rounded-xl border border-white/80 bg-white/70 px-3 py-2.5 text-xs font-semibold text-slate-700 shadow-sm backdrop-blur sm:col-span-3 sm:text-sm">
              <Clock3 size={16} className="shrink-0 text-emerald-700" aria-hidden="true" />
              <span>Two convenient locations in Sec-44 &amp; Sec-82 · Clinic timings vary by location</span>
            </div>
          </div>
        </div>

        <div className="relative mx-auto w-full max-w-2xl">
          <div className="absolute -inset-5 rounded-[2.5rem] bg-[radial-gradient(circle_at_35%_25%,_rgba(52,211,153,0.34),_transparent_48%),linear-gradient(135deg,_#d6f1e7,_#eef4f0_55%,_#dceae3)]" aria-hidden="true" />
          <div className="relative aspect-[4/4.15] overflow-hidden rounded-3xl border border-white/70 bg-emerald-50 shadow-2xl shadow-slate-900/15 sm:aspect-[4/3.6]">
            <img src={DENTAL_IMAGES.doctorHero} alt="Dental care at Chauhan Dental Clinic" loading="eager"
              onError={(event) => {
                event.currentTarget.onerror = null;
                event.currentTarget.src = DENTAL_IMAGES.doctorHeroFallback;
              }}
              className="h-full w-full object-cover object-center" />
            <div className="absolute inset-0 bg-gradient-to-t from-slate-950/35 via-transparent to-emerald-950/10" aria-hidden="true" />
            <div className="absolute left-3 top-3 flex max-w-[80%] items-center gap-2 rounded-2xl border border-white/60 bg-white/75 px-3 py-2.5 text-xs font-bold leading-5 text-slate-800 shadow-lg backdrop-blur-xl sm:left-5 sm:top-5 sm:px-4 sm:text-sm">
              <span className="grid h-8 w-8 shrink-0 place-items-center rounded-xl bg-emerald-100 text-lg" aria-hidden="true">🏥</span>
              Dual Clinic Locations <span className="hidden sm:inline">(Chhalera &amp; Madhuban)</span>
              <span className="sm:hidden">(Noida)</span>
            </div>
            <div className="absolute bottom-3 right-3 flex items-center gap-2 rounded-2xl border border-white/60 bg-white/80 px-3 py-2.5 text-xs font-bold text-slate-800 shadow-lg backdrop-blur-xl sm:bottom-5 sm:right-5 sm:px-4 sm:text-sm">
              <span className="grid h-8 w-8 place-items-center rounded-xl bg-emerald-100 text-lg" aria-hidden="true">🦷</span>
              15,000+ Happy Smiles Restored
            </div>
          </div>
          
        </div>
      </div>

      <div className="mx-auto mt-12 max-w-7xl px-5 sm:px-8">
        <PostCareCarousel />
      </div>
    </section>
  );
}

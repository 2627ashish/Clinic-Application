import { LayoutDashboard, Stethoscope, UserRound } from 'lucide-react';
import ClinicLogo from './ClinicLogo';

export type ClinicPage = 'patient' | 'patientPortal' | 'doctor';

interface NavbarProps {
  activePage: ClinicPage;
  onNavigate: (page: ClinicPage) => void;
}

export default function Navbar({ activePage, onNavigate }: NavbarProps) {
  return (
    <header className="sticky top-0 z-30 border-b border-clinic-ink/5 bg-white/90 backdrop-blur">
      <div className="mx-auto flex max-w-7xl flex-col gap-3 px-5 py-3 sm:flex-row sm:items-center sm:justify-between sm:px-8">
        <button className="flex items-center gap-3 text-left" onClick={() => onNavigate('patient')}>
          <ClinicLogo className="h-12 w-20 shrink-0" />
          <span>
            <span className="block text-base font-bold tracking-tight text-clinic-ink">Chauhan Dental Clinic</span>
            <span className="block text-[10px] text-slate-500">Care that makes you smile • Your smile, our commitment</span>
          </span>
        </button>
        <nav className="flex w-full items-center justify-between gap-1 rounded-full bg-clinic-paper p-1 sm:w-auto sm:justify-start" aria-label="Main navigation">
          <button
            onClick={() => onNavigate('patient')}
            className={`flex min-h-11 flex-1 items-center justify-center gap-2 rounded-full px-2 py-2 text-sm font-semibold transition sm:flex-none sm:px-4 ${
              activePage === 'patient' ? 'bg-white text-clinic-green shadow-sm' : 'text-slate-500 hover:text-clinic-ink'
            }`}
          >
            <Stethoscope size={16} />
            <span className="hidden sm:inline">Patient Booking</span>
            <span className="sm:hidden">Booking</span>
          </button>
          <button
            onClick={() => onNavigate('patientPortal')}
            className={`flex min-h-11 flex-1 items-center justify-center gap-2 rounded-full px-2 py-2 text-sm font-semibold transition sm:flex-none sm:px-4 ${
              activePage === 'patientPortal' ? 'bg-white text-clinic-green shadow-sm' : 'text-slate-500 hover:text-clinic-ink'
            }`}
          >
            <UserRound size={16} />
            <span className="hidden sm:inline">Patient Portal</span>
            <span className="sm:hidden">Portal</span>
          </button>
          <button
            onClick={() => onNavigate('doctor')}
            className={`flex min-h-11 flex-1 items-center justify-center gap-2 rounded-full px-2 py-2 text-sm font-semibold transition sm:flex-none sm:px-4 ${
              activePage === 'doctor' ? 'bg-white text-clinic-green shadow-sm' : 'text-slate-500 hover:text-clinic-ink'
            }`}
          >
            <LayoutDashboard size={16} />
            <span className="hidden sm:inline">Doctor Login</span>
            <span className="sm:hidden">Doctor</span>
          </button>
        </nav>
      </div>
    </header>
  );
}

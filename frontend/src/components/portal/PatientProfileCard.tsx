import { LogOut } from 'lucide-react';
import type { PatientProfileView } from '../../api/client';
import { calculateAge } from '../../utils/demographics';

interface PatientProfileCardProps {
  profile: PatientProfileView | null;
  patientName: string;
  onSignOut: () => void;
}

function formatDate(date: string | null): string {
  if (!date) return 'Not recorded';
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'long',
    timeZone: 'UTC',
  }).format(new Date(`${date}T00:00:00Z`));
}

export default function PatientProfileCard({ profile, patientName, onSignOut }: PatientProfileCardProps) {
  const age = profile?.dateOfBirth ? calculateAge(profile.dateOfBirth) : null;

  return (
    <header className="rounded-3xl border border-slate-100 bg-white p-6 shadow-sm sm:p-8">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-center">
        <div>
          <p className="text-sm font-bold uppercase tracking-wider text-clinic-green">Patient portal</p>
          <h1 className="mt-2 text-2xl font-bold text-clinic-ink">Namaste, {patientName}!</h1>
        </div>
        <button type="button" onClick={onSignOut}
          className="inline-flex min-h-11 items-center gap-2 self-start rounded-xl border border-slate-200 px-4 font-semibold text-slate-600 sm:self-auto">
          <LogOut size={16} aria-hidden="true" /> Sign out
        </button>
      </div>
      <dl className="mt-5 grid gap-3 border-t border-slate-100 pt-4 text-sm sm:grid-cols-2 lg:grid-cols-4">
        <div>
          <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">Phone</dt>
          <dd className="mt-1 font-semibold text-slate-800">{profile?.phoneNumber ?? 'Loading…'}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">Date of birth</dt>
          <dd className="mt-1 font-semibold text-slate-800">{formatDate(profile?.dateOfBirth ?? null)}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">Age</dt>
          <dd className="mt-1 font-semibold text-slate-800">{age == null ? 'Not recorded' : `${age} years`}</dd>
        </div>
        <div>
          <dt className="text-xs font-semibold uppercase tracking-wide text-slate-500">Gender</dt>
          <dd className="mt-1 font-semibold text-slate-800">{profile?.gender || 'Not recorded'}</dd>
        </div>
      </dl>
    </header>
  );
}

import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { LockKeyhole, Stethoscope } from 'lucide-react';
import {
  changeDoctorPassword,
  doctorLogin,
  getDoctorSetupStatus,
  setupDoctorAccount,
} from '../api/client';
import DoctorDashboard from './DoctorDashboard';

const TOKEN_KEY = 'doctor_auth_token';
const PASSWORD_CHANGE_KEY = 'doctor_password_change_required';

function DoctorLogin({ onLogin }: { onLogin: (token: string, changeRequired: boolean) => void }) {
  const [username, setUsername] = useState('drchauhan');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      const session = await doctorLogin(username.trim(), password);
      onLogin(session.token, session.passwordChangeRequired);
    } catch {
      setError('Login failed. Check your username and password, then try again.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="mx-auto max-w-md px-5 py-12 sm:py-20">
      <form onSubmit={(event) => void submit(event)} className="rounded-3xl border border-slate-100 bg-white p-6 shadow-sm sm:p-8">
        <div className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-clinic-mint text-clinic-green"><Stethoscope size={26} /></div>
        <h1 className="mt-5 text-center text-2xl font-bold text-clinic-ink">Doctor Login</h1>
        <p className="mt-2 text-center text-sm text-slate-500">Dr. Chauhan Dental Clinic</p>
        <p className="mt-2 text-center text-xs text-slate-500">Use the clinic administrator-provisioned password.</p>
        {error && <p role="alert" className="mt-5 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
        <label className="mt-6 block text-sm font-semibold text-clinic-ink">Username
          <input required autoComplete="username" value={username} onChange={(event) => setUsername(event.target.value)}
            className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 px-3 font-normal" />
        </label>
        <label className="mt-4 block text-sm font-semibold text-clinic-ink">Password
          <input required type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)}
            className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 px-3 font-normal" />
        </label>
        <button disabled={busy} className="mt-6 min-h-12 w-full rounded-xl bg-clinic-green px-4 font-semibold text-white hover:bg-clinic-ink disabled:opacity-60">
          {busy ? 'Signing in…' : 'Sign in'}
        </button>
      </form>
    </main>
  );
}

function DoctorSetup({ onCreated }: { onCreated: (token: string, changeRequired: boolean) => void }) {
  const [username, setUsername] = useState('drchauhan');
  const [fullName, setFullName] = useState('Dr. B.S. Chauhan');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (password.length < 12) return setError('Use at least 12 characters for your password.');
    if (password !== confirm) return setError('The passwords do not match.');
    setBusy(true);
    setError('');
    try {
      const session = await setupDoctorAccount(username.trim(), password, fullName.trim(), email.trim());
      onCreated(session.token, session.passwordChangeRequired);
    } catch {
      setError('Account setup failed. Setup may already be complete, or the API/database may be unavailable.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="mx-auto max-w-md px-5 py-12 sm:py-20">
      <form onSubmit={(event) => void submit(event)} className="rounded-3xl border border-slate-100 bg-white p-6 shadow-sm sm:p-8">
        <span className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-clinic-mint text-clinic-green"><LockKeyhole size={25} /></span>
        <h1 className="mt-5 text-center text-2xl font-bold text-clinic-ink">Create Doctor Account</h1>
        <p className="mt-2 text-center text-sm text-slate-500">First-time setup. Your password is stored securely in the clinic database.</p>
        {error && <p role="alert" className="mt-5 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
        <label className="mt-5 block text-sm font-semibold text-clinic-ink">Username
          <input required minLength={3} maxLength={30} pattern="[A-Za-z0-9._-]+" autoComplete="username"
            value={username} onChange={(event) => setUsername(event.target.value)}
            className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 px-3 font-normal" />
        </label>
        <label className="mt-4 block text-sm font-semibold text-clinic-ink">Doctor name
          <input required maxLength={100} value={fullName} onChange={(event) => setFullName(event.target.value)}
            className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 px-3 font-normal" />
        </label>
        <label className="mt-4 block text-sm font-semibold text-clinic-ink">Email (optional)
          <input type="email" maxLength={254} autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)}
            className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 px-3 font-normal" />
        </label>
        <label className="mt-4 block text-sm font-semibold text-clinic-ink">Password (at least 12 characters)
          <input required minLength={12} maxLength={128} type="password" autoComplete="new-password" value={password}
            onChange={(event) => setPassword(event.target.value)}
            className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 px-3 font-normal" />
        </label>
        <label className="mt-4 block text-sm font-semibold text-clinic-ink">Confirm password
          <input required type="password" autoComplete="new-password" value={confirm}
            onChange={(event) => setConfirm(event.target.value)}
            className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 px-3 font-normal" />
        </label>
        <button disabled={busy} className="mt-6 min-h-12 w-full rounded-xl bg-clinic-green px-4 font-semibold text-white disabled:opacity-60">
          {busy ? 'Creating account…' : 'Create account and continue'}
        </button>
      </form>
    </main>
  );
}

function PasswordForm({ onComplete, onCancel }: { onComplete: () => void; onCancel: () => void }) {
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (next.length < 12) return setError('Use at least 12 characters for your new password.');
    if (next !== confirm) return setError('The new passwords do not match.');
    setBusy(true);
    setError('');
    try {
      await changeDoctorPassword(current, next);
      onComplete();
    } catch {
      setError('Password could not be changed. Check the current password and try again.');
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="mx-auto max-w-md px-5 py-12 sm:py-20">
      <form onSubmit={(event) => void submit(event)} className="rounded-3xl border border-slate-100 bg-white p-6 shadow-sm sm:p-8">
        <span className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-clinic-mint text-clinic-green"><LockKeyhole size={25} /></span>
        <h1 className="mt-5 text-center text-2xl font-bold text-clinic-ink">Change Password</h1>
        <p className="mt-2 text-center text-sm text-slate-500">Choose a password with at least 12 characters.</p>
        {error && <p role="alert" className="mt-5 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
        <label className="mt-5 block text-sm font-semibold text-clinic-ink">Current password
          <input required type="password" autoComplete="current-password" value={current} onChange={(event) => setCurrent(event.target.value)}
            className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 px-3 font-normal" />
        </label>
        <label className="mt-4 block text-sm font-semibold text-clinic-ink">New password
          <input required minLength={12} type="password" autoComplete="new-password" value={next} onChange={(event) => setNext(event.target.value)}
            className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 px-3 font-normal" />
        </label>
        <label className="mt-4 block text-sm font-semibold text-clinic-ink">Confirm new password
          <input required type="password" autoComplete="new-password" value={confirm} onChange={(event) => setConfirm(event.target.value)}
            className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 px-3 font-normal" />
        </label>
        <button disabled={busy} className="mt-6 min-h-12 w-full rounded-xl bg-clinic-green px-4 font-semibold text-white disabled:opacity-60">
          {busy ? 'Updating…' : 'Update password'}
        </button>
        <button type="button" onClick={onCancel} className="mt-3 min-h-11 w-full rounded-xl border border-slate-200 font-semibold text-slate-600">Cancel / Sign out</button>
      </form>
    </main>
  );
}

export default function DoctorPortal() {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) ?? '');
  const [mustChange, setMustChange] = useState(() => localStorage.getItem(PASSWORD_CHANGE_KEY) === 'true');
  const [showSettings, setShowSettings] = useState(false);
  const [setupRequired, setSetupRequired] = useState<boolean | null>(null);
  const [setupError, setSetupError] = useState('');

  useEffect(() => {
    if (token) return;
    void getDoctorSetupStatus()
      .then((status) => {
        setSetupRequired(status.setupRequired);
        setSetupError('');
      })
      .catch(() => {
        setSetupError('Cannot reach the doctor account service. Check that the backend and MongoDB are running.');
      });
  }, [token]);

  function acceptLogin(nextToken: string, changeRequired: boolean) {
    localStorage.setItem(TOKEN_KEY, nextToken);
    localStorage.setItem(PASSWORD_CHANGE_KEY, String(changeRequired));
    setToken(nextToken);
    setMustChange(changeRequired);
  }

  function signOut() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(PASSWORD_CHANGE_KEY);
    setToken('');
    setMustChange(false);
    setShowSettings(false);
  }

  function passwordChanged() {
    signOut();
  }

  if (!token && setupError) {
    return <main className="mx-auto max-w-md px-5 py-12">
      <p role="alert" className="rounded-xl bg-rose-50 p-4 text-sm text-rose-700">{setupError}</p>
    </main>;
  }
  if (!token && setupRequired === null) {
    return <main className="mx-auto max-w-md px-5 py-12 text-center text-sm text-slate-500">Checking doctor account setup…</main>;
  }
  if (!token && setupRequired) return <DoctorSetup onCreated={acceptLogin} />;
  if (!token) return <DoctorLogin onLogin={acceptLogin} />;
  if (mustChange || showSettings) return <PasswordForm onComplete={passwordChanged} onCancel={signOut} />;
  return <DoctorDashboard onOpenSettings={() => setShowSettings(true)} onLogout={signOut} />;
}

import { useEffect, useRef, useState } from 'react';
import type { ClipboardEvent, FormEvent, KeyboardEvent } from 'react';
import axios from 'axios';
import { HeartPulse, LoaderCircle, Pencil, RefreshCw } from 'lucide-react';
import { requestPatientOtp, verifyPatientOtp } from '../api/client';
import type { PatientPortalSession } from '../api/client';

interface PatientOtpLoginProps {
  onSignedIn: (session: PatientPortalSession) => void;
}

type LoginStep = 'phone' | 'otp' | 'patient';

export default function PatientOtpLogin({ onSignedIn }: PatientOtpLoginProps) {
  const [phone, setPhone] = useState('');
  const [otpDigits, setOtpDigits] = useState(['', '', '', '']);
  const [demoCode, setDemoCode] = useState('');
  const [patientOptions, setPatientOptions] = useState<PatientPortalSession[]>([]);
  const [step, setStep] = useState<LoginStep>('phone');
  const [cooldown, setCooldown] = useState(0);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [sending, setSending] = useState(false);
  const inputRefs = useRef<Array<HTMLInputElement | null>>([]);
  const code = otpDigits.join('');

  useEffect(() => {
    if (step === 'otp') inputRefs.current[0]?.focus();
  }, [step]);

  useEffect(() => {
    if (cooldown <= 0) return undefined;
    const timer = window.setTimeout(() => setCooldown((value) => Math.max(0, value - 1)), 1000);
    return () => window.clearTimeout(timer);
  }, [cooldown]);

  async function sendOtp(isResend: boolean) {
    setSending(true);
    setError('');
    setNotice('');
    try {
      const response = await requestPatientOtp(phone);
      if (!response.success) throw new Error('The OTP could not be sent. Please try again.');
      setStep('otp');
      setOtpDigits(['', '', '', '']);
      setDemoCode(response.mockOtp ?? '');
      setCooldown(response.resendCooldown);
      setNotice(isResend
        ? 'New OTP sent to your WhatsApp! Previous code is now invalid.'
        : response.message);
    } catch (requestError: unknown) {
      setError(errorMessage(requestError, 'We could not send a code. Check the phone number and try again.'));
    } finally {
      setSending(false);
    }
  }

  async function verify(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSending(true);
    setError('');
    try {
      const session = await verifyPatientOtp(phone, code);
      if (session.patients.length === 0) {
        throw new Error('No patient profile was found for this phone number.');
      }
      if (session.patients.length === 1) onSignedIn(session.patients[0]);
      else {
        setPatientOptions(session.patients);
        setStep('patient');
      }
    } catch (verifyError: unknown) {
      setError(errorMessage(verifyError, 'Could not verify the code. Check it is current and for this number.'));
    } finally {
      setSending(false);
    }
  }

  function editNumber() {
    setStep('phone');
    setOtpDigits(['', '', '', '']);
    setDemoCode('');
    setCooldown(0);
    setError('');
    setNotice('');
  }

  function updateDigit(index: number, value: string) {
    const cleanDigits = value.replace(/\D/g, '');
    if (cleanDigits.length > 1) {
      const pasted = cleanDigits.slice(0, 4);
      setOtpDigits([...pasted.padEnd(4, ' ')].map((digit) => digit.trim()));
      inputRefs.current[Math.min(pasted.length, 3)]?.focus();
      return;
    }
    const next = [...otpDigits];
    next[index] = cleanDigits;
    setOtpDigits(next);
    if (cleanDigits && index < 3) inputRefs.current[index + 1]?.focus();
  }

  function pasteCode(event: ClipboardEvent<HTMLInputElement>) {
    event.preventDefault();
    const pasted = event.clipboardData.getData('text').replace(/\D/g, '').slice(0, 4);
    setOtpDigits([...pasted.padEnd(4, ' ')]
      .slice(0, 4)
      .map((digit) => digit.trim()));
    inputRefs.current[Math.min(pasted.length, 3)]?.focus();
  }

  function handleKeyDown(index: number, event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === 'Backspace' && !otpDigits[index] && index > 0) inputRefs.current[index - 1]?.focus();
  }

  return (
    <section className="mx-auto max-w-md rounded-3xl border border-slate-100 bg-white p-6 shadow-sm sm:p-8">
      <span className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-clinic-mint text-clinic-green">
        <HeartPulse size={26} />
      </span>
      <h1 className="mt-5 text-center text-2xl font-bold text-clinic-ink">Patient Portal</h1>
      <p className="mt-2 text-center text-sm text-slate-500">Sign in to view your visits and prescriptions.</p>
      {error && <p role="alert" className="mt-5 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
      {notice && <p role="status" className="mt-5 rounded-xl bg-clinic-mint p-3 text-sm text-clinic-green">{notice}</p>}

      {step === 'phone' ? (
        <>
          <label className="mt-6 block text-sm font-semibold text-clinic-ink">WhatsApp phone number
            <span className="mt-1.5 flex min-h-11 items-center rounded-xl border border-slate-200 focus-within:border-clinic-green">
              <span className="border-r border-slate-200 px-3 text-slate-500">+91</span>
              <input required inputMode="tel" autoComplete="tel-national" maxLength={16} pattern="[0-9]{10}"
                value={phone} onChange={(event) => {
                  const rawDigits = event.target.value.replace(/\D/g, '');
                  const localDigits = rawDigits.length === 12 && rawDigits.startsWith('91')
                    ? rawDigits.slice(2)
                    : rawDigits.length === 11 && rawDigits.startsWith('0')
                      ? rawDigits.slice(1)
                      : rawDigits.slice(0, 10);
                  setPhone(localDigits.slice(0, 10));
                }}
                className="min-w-0 flex-1 rounded-r-xl px-3 font-normal outline-none" />
            </span>
          </label>
          <button type="button" disabled={sending || phone.length !== 10} onClick={() => void sendOtp(false)}
            className="mt-5 flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-clinic-green px-4 font-semibold text-white disabled:opacity-50">
            {sending && <LoaderCircle size={17} className="animate-spin" />}
            {sending ? 'Sending…' : 'Send WhatsApp OTP'}
          </button>
        </>
      ) : step === 'otp' ? (
        <form onSubmit={(event) => void verify(event)}>
          <div className="mt-6 flex items-center justify-center gap-2 text-sm text-slate-600">
            <span>Code sent to <strong className="text-clinic-ink">+91 {phone}</strong></span>
            <button type="button" onClick={editNumber} className="inline-flex items-center gap-1 font-semibold text-clinic-green">
              <Pencil size={14} /> Edit Number
            </button>
          </div>
          <fieldset className="mt-5">
            <legend className="mb-2 w-full text-center text-sm font-semibold text-clinic-ink">Enter your 4-digit OTP</legend>
            <div className="flex justify-center gap-3">
              {[0, 1, 2, 3].map((index) => (
                <input key={index} ref={(element) => { inputRefs.current[index] = element; }}
                  aria-label={`OTP digit ${index + 1}`} inputMode="numeric" autoComplete={index === 0 ? 'one-time-code' : 'off'}
                  maxLength={1} value={otpDigits[index]} onChange={(event) => updateDigit(index, event.target.value)}
                  onPaste={pasteCode} onKeyDown={(event) => handleKeyDown(index, event)}
                  className="h-12 w-12 rounded-xl border border-slate-200 text-center text-xl font-bold text-clinic-ink focus:border-clinic-green focus:outline-none focus:ring-2 focus:ring-clinic-green/20" />
              ))}
            </div>
          </fieldset>
          {demoCode && <p className="mt-4 rounded-xl border border-amber-200 bg-amber-50 p-3 text-center text-sm text-amber-800">Demo Code: [{demoCode}]</p>}
          <button disabled={sending || !/^\d{4}$/.test(code)}
            className="mt-5 flex min-h-12 w-full items-center justify-center gap-2 rounded-xl bg-clinic-green px-4 font-semibold text-white disabled:opacity-50">
            {sending && <LoaderCircle size={17} className="animate-spin" />}
            {sending ? 'Verifying…' : 'Verify and continue'}
          </button>
          <button type="button" disabled={sending || cooldown > 0} onClick={() => void sendOtp(true)}
            className="mt-3 flex min-h-11 w-full items-center justify-center gap-2 rounded-xl border border-slate-200 font-semibold text-clinic-green disabled:text-slate-400">
            <RefreshCw size={15} />
            {cooldown > 0 ? `Resend OTP in ${cooldown}s…` : 'Resend OTP on WhatsApp'}
          </button>
        </form>
      ) : (
        <div className="mt-6">
          <p className="text-center text-sm text-slate-600">More than one patient profile uses this number. Choose a profile to continue.</p>
          <div className="mt-4 space-y-2">
            {patientOptions.map((patient) => (
              <button key={patient.token} type="button" onClick={() => onSignedIn(patient)}
                className="min-h-12 w-full rounded-xl border border-slate-200 px-4 text-left font-semibold text-clinic-ink hover:border-clinic-green hover:bg-clinic-mint">
                {patient.patientName}
              </button>
            ))}
          </div>
          <button type="button" onClick={editNumber}
            className="mt-3 min-h-11 w-full rounded-xl border border-slate-200 font-semibold text-slate-600">Edit Number</button>
        </div>
      )}
    </section>
  );
}

function errorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError<{ message?: string; detail?: string }>(error)) {
    return error.response?.data?.message ?? error.response?.data?.detail ?? fallback;
  }
  return error instanceof Error ? error.message : fallback;
}

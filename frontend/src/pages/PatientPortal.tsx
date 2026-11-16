import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import axios from 'axios';
import { CalendarDays, Download, HeartPulse, LoaderCircle, LogOut } from 'lucide-react';
import {
  downloadPatientDocument,
  downloadPatientPrescription,
  getPatientDocuments,
  getPatientVisits,
  requestPatientOtp,
  verifyPatientOtp,
} from '../api/client';
import type { ClinicPage } from '../components/Navbar';
import type { PatientVisitSummary } from '../api/client';
import type { PatientDocumentView } from '../types';

const TOKEN_KEY = 'patient_portal_token';
const NAME_KEY = 'patient_portal_name';
const PATIENT_IDLE_TIMEOUT_MS = 30 * 60 * 1000;

export default function PatientPortal({ onNavigate }: { onNavigate: (page: ClinicPage) => void }) {
  const [phone, setPhone] = useState('');
  const [code, setCode] = useState('');
  const [demoCode, setDemoCode] = useState('');
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) ?? '');
  const [patientName, setPatientName] = useState(() => localStorage.getItem(NAME_KEY) ?? '');
  const [visits, setVisits] = useState<PatientVisitSummary[]>([]);
  const [documents, setDocuments] = useState<PatientDocumentView[]>([]);
  const [documentPreviews, setDocumentPreviews] = useState<Record<string, string>>({});
  const [portalTab, setPortalTab] = useState<'prescriptions' | 'xrays' | 'other'>('prescriptions');
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);
  const [sending, setSending] = useState(false);

  useEffect(() => {
    if (!token) return;
    let active = true;
    setLoading(true);
    void getPatientVisits(token)
      .then((items) => { if (active) setVisits(items); })
      .catch(() => {
        if (active) {
          setError('Your session expired or visit history could not be loaded. Please sign in again.');
          signOut();
        }
      })
      .finally(() => { if (active) setLoading(false); });
    void getPatientDocuments(token)
      .then(async (items) => {
        if (active) setDocuments(items);
        const imageItems = items.filter((item) => item.contentType.startsWith('image/'));
        const previews = await Promise.all(imageItems.map(async (item) => {
          const blob = await downloadPatientDocument(token, item.id);
          return [item.id, URL.createObjectURL(blob)] as const;
        }));
        if (active) setDocumentPreviews(Object.fromEntries(previews));
        else previews.forEach(([, url]) => URL.revokeObjectURL(url));
      })
      .catch(() => { if (active) setError('Patient documents could not be loaded. Please refresh or sign in again.'); });
    return () => { active = false; };
  }, [token]);

  useEffect(() => () => {
    Object.values(documentPreviews).forEach((url) => URL.revokeObjectURL(url));
  }, [documentPreviews]);

  useEffect(() => {
    if (!token) return;
    let timer = 0;
    const resetIdleTimeout = () => {
      window.clearTimeout(timer);
      timer = window.setTimeout(signOut, PATIENT_IDLE_TIMEOUT_MS);
    };
    const events: (keyof WindowEventMap)[] = ['pointerdown', 'keydown', 'touchstart', 'scroll'];
    events.forEach((name) => window.addEventListener(name, resetIdleTimeout, { passive: true }));
    resetIdleTimeout();
    return () => {
      window.clearTimeout(timer);
      events.forEach((name) => window.removeEventListener(name, resetIdleTimeout));
    };
  }, [token]);

  async function sendCode() {
    setSending(true);
    setError('');
    setNotice('');
    setDemoCode('');
    try {
      const response = await requestPatientOtp(phone);
      setNotice(response.message);
      setDemoCode(response.demoCode ?? '');
    } catch {
      setError('We could not send a code. Enter a valid phone number and try again.');
    } finally {
      setSending(false);
    }
  }

  async function signIn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError('');
    setSending(true);
    try {
      const session = await verifyPatientOtp(phone, code);
      localStorage.setItem(TOKEN_KEY, session.token);
      localStorage.setItem(NAME_KEY, session.patientName);
      setToken(session.token);
      setPatientName(session.patientName);
    } catch (error: unknown) {
      const message = axios.isAxiosError<{ message?: string }>(error)
        ? error.response?.data?.message
        : undefined;
      setError(message ?? 'Could not verify the code. Check it is current and was requested for this phone number.');
    } finally {
      setSending(false);
    }
  }

  function signOut() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(NAME_KEY);
    setToken('');
    setPatientName('');
    setVisits([]);
    setDocuments([]);
    setDocumentPreviews({});
    setDemoCode('');
    setCode('');
  }

  async function download(appointmentId: string) {
    setError('');
    try {
      const blob = await downloadPatientPrescription(token, appointmentId);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `prescription-${appointmentId}.pdf`;
      link.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch {
      setError('The prescription could not be downloaded. Please try again.');
    }
  }

  async function downloadDocument(documentId: string, fileName: string) {
    setError('');
    try {
      const blob = await downloadPatientDocument(token, documentId);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      link.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch {
      setError('The document could not be downloaded. Please try again.');
    }
  }

  const visibleDocuments = documents.filter((document) => portalTab === 'prescriptions'
    ? document.category === 'EXTERNAL_PRESCRIPTION' || document.category === 'HANDWRITTEN_RX'
    : portalTab === 'xrays'
      ? document.category === 'XRAY_RVG'
      : document.category === 'LAB_REPORT' || document.category === 'OTHER');

  return (
    <main className="mx-auto max-w-3xl px-5 py-8 sm:py-12">
      {!token ? (
        <section className="mx-auto max-w-md rounded-3xl border border-slate-100 bg-white p-6 shadow-sm sm:p-8">
          <span className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-clinic-mint text-clinic-green"><HeartPulse size={26} /></span>
          <h1 className="mt-5 text-center text-2xl font-bold text-clinic-ink">Patient Portal</h1>
          <p className="mt-2 text-center text-sm text-slate-500">Sign in to view your visits and prescriptions.</p>
          {error && <p role="alert" className="mt-5 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
          {notice && <p role="status" className="mt-5 rounded-xl bg-clinic-mint p-3 text-sm text-clinic-green">{notice}</p>}
          {demoCode && <p className="mt-3 rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm font-semibold text-amber-800">Demo Verification Code: {demoCode}</p>}
          <label className="mt-6 block text-sm font-semibold text-clinic-ink">10-digit phone number
            <input required inputMode="tel" autoComplete="tel" maxLength={10} pattern="[0-9]{10}" value={phone}
              onChange={(event) => setPhone(event.target.value.replace(/\D/g, '').slice(0, 10))}
              className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 px-3 font-normal" />
          </label>
          {!notice ? (
            <button type="button" disabled={sending || phone.length !== 10} onClick={() => void sendCode()}
              className="mt-5 min-h-12 w-full rounded-xl bg-clinic-green px-4 font-semibold text-white disabled:opacity-50">
              {sending ? 'Sending…' : 'Send WhatsApp OTP'}
            </button>
          ) : (
            <form onSubmit={(event) => void signIn(event)}>
              <label className="mt-4 block text-sm font-semibold text-clinic-ink">4-digit verification code
                <input required inputMode="numeric" pattern="[0-9]{4}" maxLength={4} value={code}
                  onChange={(event) => setCode(event.target.value.replace(/\D/g, '').slice(0, 4))}
                  className="mt-1.5 min-h-11 w-full rounded-xl border border-slate-200 px-3 font-normal" />
              </label>
              <button disabled={sending || code.length !== 4} className="mt-5 min-h-12 w-full rounded-xl bg-clinic-green px-4 font-semibold text-white disabled:opacity-50">
                {sending ? 'Verifying…' : 'Verify and continue'}
              </button>
              <button type="button" onClick={() => { setNotice(''); setDemoCode(''); setCode(''); }}
                className="mt-3 min-h-11 w-full rounded-xl border border-slate-200 font-semibold text-slate-600">Use another number</button>
            </form>
          )}
        </section>
      ) : (
        <>
          <header className="flex flex-col justify-between gap-4 rounded-3xl bg-white p-6 shadow-sm sm:flex-row sm:items-center sm:p-8">
            <div>
              <p className="text-sm font-bold uppercase tracking-wider text-clinic-green">Patient portal</p>
              <h1 className="mt-2 text-2xl font-bold text-clinic-ink">Namaste, {patientName}!</h1>
              <p className="mt-2 text-sm text-slate-500">How is your dental health today?</p>
            </div>
            <button onClick={signOut} className="inline-flex min-h-11 items-center gap-2 self-start rounded-xl border border-slate-200 px-4 font-semibold text-slate-600 sm:self-auto"><LogOut size={16} /> Sign out</button>
          </header>
          <section className="mt-6 overflow-hidden rounded-3xl border border-slate-100 bg-white shadow-sm">
            <div className="flex flex-wrap gap-2 border-b border-slate-100 p-4" role="tablist" aria-label="Patient records">
              {([
                ['prescriptions', 'Official Prescriptions & Bills'],
                ['xrays', 'Dental X-Rays'],
                ['other', 'Lab & Other Docs'],
              ] as const).map(([id, label]) => (
                <button key={id} type="button" role="tab" aria-selected={portalTab === id} onClick={() => setPortalTab(id)}
                  className={`rounded-xl px-4 py-2.5 text-sm font-semibold ${portalTab === id ? 'bg-clinic-green text-white' : 'bg-clinic-paper text-slate-600'}`}>{label}</button>
              ))}
            </div>
            <div className="p-5 sm:p-7">
            {portalTab === 'prescriptions' && <h2 className="flex items-center gap-2 text-lg font-bold text-clinic-ink"><CalendarDays size={20} className="text-clinic-green" /> Official Prescriptions &amp; Bills</h2>}
            {loading ? (
              <p className="flex items-center gap-2 py-10 text-sm text-slate-500"><LoaderCircle size={17} className="animate-spin" /> Loading visits…</p>
            ) : portalTab === 'prescriptions' && visits.length === 0 && visibleDocuments.length === 0 ? (
              <p className="py-10 text-center text-sm text-slate-500">No completed consultations are available yet.</p>
            ) : portalTab === 'prescriptions' ? (
              <div className="mt-4 space-y-3">
                {visibleDocuments.map((document) => (
                  <article key={document.id} className="flex flex-col justify-between gap-3 rounded-2xl border border-slate-100 p-4 sm:flex-row sm:items-center">
                    <div className="flex items-center gap-3">
                      {documentPreviews[document.id] ? <img src={documentPreviews[document.id]} alt="" className="h-14 w-14 rounded-lg object-cover" /> : <span className="grid h-12 w-12 place-items-center rounded-xl bg-clinic-mint text-clinic-green">Rx</span>}
                      <div><p className="font-semibold text-clinic-ink">{document.fileName}</p><p className="text-xs text-slate-500">{document.category.replaceAll('_', ' ')}</p></div>
                    </div>
                    <button onClick={() => void downloadDocument(document.id, document.fileName)} className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl bg-clinic-mint px-4 text-sm font-semibold text-clinic-green"><Download size={15} />Download</button>
                  </article>
                ))}
                {visits.map((visit) => (
                  <article key={visit.appointmentId} className="rounded-2xl border border-slate-100 p-4">
                    <p className="text-xs font-semibold uppercase tracking-wide text-clinic-green">
                      {new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(visit.visitDate))}
                    </p>
                    <p className="mt-2 text-sm text-clinic-ink"><strong>Diagnosis:</strong> {visit.diagnosis}</p>
                    <p className="mt-1 text-sm text-clinic-ink"><strong>Procedure:</strong> {visit.procedurePerformed}</p>
                    <p className="mt-1 text-xs text-slate-500">Dr. B.S. Chauhan</p>
                    <button onClick={() => void download(visit.appointmentId)}
                      className="mt-3 inline-flex min-h-11 items-center gap-2 rounded-xl bg-clinic-mint px-4 text-sm font-semibold text-clinic-green">
                      <Download size={16} /> Download Prescription PDF
                    </button>
                  </article>
                ))}
              </div>
            ) : visibleDocuments.length === 0 ? (
              <p className="py-10 text-center text-sm text-slate-500">No documents in this category yet.</p>
            ) : (
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                {visibleDocuments.map((document) => (
                  <article key={document.id} className="overflow-hidden rounded-2xl border border-slate-100">
                    {documentPreviews[document.id] ? <img src={documentPreviews[document.id]} alt={`Preview of ${document.fileName}`} className="h-40 w-full bg-clinic-paper object-contain" /> : <div className="grid h-32 place-items-center bg-clinic-paper text-sm font-semibold text-clinic-green">{document.contentType === 'application/pdf' ? 'PDF document' : document.category.replaceAll('_', ' ')}</div>}
                    <div className="flex items-center justify-between gap-3 p-3"><p className="truncate text-sm font-semibold text-clinic-ink">{document.fileName}</p><button aria-label={`Download ${document.fileName}`} onClick={() => void downloadDocument(document.id, document.fileName)} className="rounded-lg bg-clinic-mint p-2 text-clinic-green"><Download size={16} /></button></div>
                  </article>
                ))}
              </div>
            )}
            </div>
          </section>
          {error && <p role="alert" className="mt-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
          <button onClick={() => onNavigate('patient')} className="mt-5 min-h-12 w-full rounded-xl bg-clinic-green px-4 font-semibold text-white sm:w-auto">Book follow-up appointment</button>
        </>
      )}
    </main>
  );
}

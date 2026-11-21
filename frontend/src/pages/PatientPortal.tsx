import { useEffect, useState } from 'react';
import { CalendarDays, Download, Eye, LoaderCircle, X } from 'lucide-react';
import {
  downloadPatientBill,
  downloadPatientHandwrittenRx,
  downloadPatientDocument,
  downloadPatientPrescription,
  getPatientDocuments,
  getPatientProfile,
  getPatientVisits,
} from '../api/client';
import PatientOtpLogin from '../components/PatientOtpLogin';
import PatientProfileCard from '../components/portal/PatientProfileCard';
import PatientVisitCard from '../components/portal/PatientVisitCard';
import type { ClinicPage } from '../components/Navbar';
import type { PatientVisitSummary } from '../api/client';
import type { PatientDocumentView } from '../types';

const TOKEN_KEY = 'patient_portal_token';
const NAME_KEY = 'patient_portal_name';
const PATIENT_IDLE_TIMEOUT_MS = 30 * 60 * 1000;

export default function PatientPortal({ onNavigate }: { onNavigate: (page: ClinicPage) => void }) {
  const [token, setToken] = useState(() => localStorage.getItem(TOKEN_KEY) ?? '');
  const [patientName, setPatientName] = useState(() => localStorage.getItem(NAME_KEY) ?? '');
  const [patientProfile, setPatientProfile] = useState<Awaited<ReturnType<typeof getPatientProfile>> | null>(null);
  const [visits, setVisits] = useState<PatientVisitSummary[]>([]);
  const [documents, setDocuments] = useState<PatientDocumentView[]>([]);
  const [documentPreviews, setDocumentPreviews] = useState<Record<string, string>>({});
  const [documentPreview, setDocumentPreview] = useState<{ fileName: string; contentType: string; url: string } | null>(null);
  const [portalTab, setPortalTab] = useState<'prescriptions' | 'xrays' | 'other'>('prescriptions');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!token) return;
    let active = true;
    setLoading(true);
    void getPatientProfile(token)
      .then((profile) => {
        if (active) {
          setPatientProfile(profile);
          setPatientName(profile.fullName);
          localStorage.setItem(NAME_KEY, profile.fullName);
        }
      })
      .catch(() => { if (active) setError('Patient details could not be loaded. Please sign in again.'); });
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
    if (!documentPreview) return;
    return () => URL.revokeObjectURL(documentPreview.url);
  }, [documentPreview]);

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

  function signOut() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(NAME_KEY);
    setToken('');
    setPatientName('');
    setPatientProfile(null);
    setVisits([]);
    setDocuments([]);
    setDocumentPreviews({});
    setDocumentPreview(null);
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

  async function downloadHandwrittenRx(appointmentId: string) {
    setError('');
    try {
      const blob = await downloadPatientHandwrittenRx(token, appointmentId);
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = blob.type === 'image/jpeg'
        ? 'Chauhan_Dental_Handwritten_Rx.jpg'
        : 'Chauhan_Dental_Handwritten_Rx.png';
      link.click();
      window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    } catch {
      setError('The handwritten prescription could not be downloaded. Please try again.');
    }
  }

  async function viewAppointmentPdf(appointmentId: string, documentType: 'prescription' | 'bill') {
    setError('');
    try {
      const blob = documentType === 'prescription'
        ? await downloadPatientPrescription(token, appointmentId)
        : await downloadPatientBill(token, appointmentId);
      setDocumentPreview({
        fileName: `${documentType}-${appointmentId}.pdf`,
        contentType: 'application/pdf',
        url: URL.createObjectURL(blob),
      });
    } catch {
      setError(`The outpatient ${documentType} could not be opened. Please try again.`);
    }
  }

  async function viewDocument(document: PatientDocumentView) {
    setError('');
    try {
      const blob = await downloadPatientDocument(token, document.id);
      setDocumentPreview({
        fileName: document.fileName,
        contentType: document.contentType,
        url: URL.createObjectURL(blob),
      });
    } catch {
      setError('The document could not be opened. Please try again.');
    }
  }

  const visibleDocuments = documents.filter((document) => portalTab === 'prescriptions'
    ? document.category === 'EXTERNAL_PRESCRIPTION'
    : portalTab === 'xrays'
      ? document.category === 'XRAY_RVG'
      : document.category === 'LAB_REPORT' || document.category === 'OTHER');

  return (
    <main className="relative min-h-screen overflow-hidden bg-slate-50 px-5 py-8 sm:py-12">
      <span aria-hidden="true" className="pointer-events-none absolute right-8 top-24 select-none text-[14rem] leading-none text-emerald-900/[0.035]">🦷</span>
      <div className="relative z-10 mx-auto max-w-3xl">
      {!token ? (
        <PatientOtpLogin onSignedIn={(session) => {
          localStorage.setItem(TOKEN_KEY, session.token);
          localStorage.setItem(NAME_KEY, session.patientName);
          setToken(session.token);
          setPatientName(session.patientName);
        }} />
      ) : (
        <>
          <PatientProfileCard profile={patientProfile} patientName={patientName} onSignOut={signOut} />
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
                      {documentPreviews[document.id]
                        ? <button type="button" onClick={() => void viewDocument(document)} aria-label={`View ${document.fileName}`}><img src={documentPreviews[document.id]} alt="" className="h-14 w-14 rounded-lg object-cover" /></button>
                        : <span className="grid h-12 w-12 place-items-center rounded-xl bg-clinic-mint text-clinic-green">Rx</span>}
                      <div><p className="font-semibold text-clinic-ink">{document.fileName}</p><p className="text-xs text-slate-500">{document.category.replaceAll('_', ' ')}</p></div>
                    </div>
                    <div className="flex gap-2">
                      <button type="button" onClick={() => void viewDocument(document)} className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl bg-clinic-mint px-3 text-sm font-semibold text-clinic-green"><Eye size={15} />View</button>
                      <button type="button" onClick={() => void downloadDocument(document.id, document.fileName)} className="inline-flex min-h-10 items-center justify-center gap-2 rounded-xl border border-slate-200 px-3 text-sm font-semibold text-slate-700"><Download size={15} />Download</button>
                    </div>
                  </article>
                ))}
                {visits.map((visit) => {
                  const handwrittenPrescription = documents.find((document) =>
                    document.appointmentId === visit.appointmentId && document.category === 'HANDWRITTEN_RX');
                  return (
                    <PatientVisitCard
                      key={visit.appointmentId}
                      visit={visit}
                      handwrittenPrescription={handwrittenPrescription}
                      handwrittenPreviewUrl={handwrittenPrescription
                        ? documentPreviews[handwrittenPrescription.id] : undefined}
                      onViewPrescription={() => void viewAppointmentPdf(visit.appointmentId, 'prescription')}
                      onViewBill={() => void viewAppointmentPdf(visit.appointmentId, 'bill')}
                      onViewHandwrittenPrescription={() => handwrittenPrescription && void viewDocument(handwrittenPrescription)}
                      onDownloadHandwrittenPrescription={() => void downloadHandwrittenRx(visit.appointmentId)}
                    />
                  );
                })}
              </div>
            ) : visibleDocuments.length === 0 ? (
              <p className="py-10 text-center text-sm text-slate-500">No documents in this category yet.</p>
            ) : (
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                {visibleDocuments.map((document) => (
                  <article key={document.id} className="overflow-hidden rounded-2xl border border-slate-100">
                    <button type="button" onClick={() => void viewDocument(document)} className="block w-full">
                      {documentPreviews[document.id] ? <img src={documentPreviews[document.id]} alt={`Preview of ${document.fileName}`} className="h-40 w-full bg-clinic-paper object-contain" /> : <div className="grid h-32 place-items-center bg-clinic-paper text-sm font-semibold text-clinic-green">{document.contentType === 'application/pdf' ? 'Open PDF' : document.category.replaceAll('_', ' ')}</div>}
                    </button>
                    <div className="flex items-center justify-between gap-3 p-3">
                      <p className="truncate text-sm font-semibold text-clinic-ink">{document.fileName}</p>
                      <div className="flex gap-2">
                        <button aria-label={`View ${document.fileName}`} onClick={() => void viewDocument(document)} className="rounded-lg bg-clinic-mint p-2 text-clinic-green"><Eye size={16} /></button>
                        <button aria-label={`Download ${document.fileName}`} onClick={() => void downloadDocument(document.id, document.fileName)} className="rounded-lg border border-slate-200 p-2 text-slate-700"><Download size={16} /></button>
                      </div>
                    </div>
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
      </div>
      {documentPreview && (
        <div className="fixed inset-0 z-[80] grid place-items-center bg-slate-950/70 p-3 sm:p-6" role="dialog" aria-modal="true" aria-label={documentPreview.fileName}>
          <section className="flex max-h-full w-full max-w-5xl flex-col overflow-hidden rounded-2xl bg-white shadow-2xl">
            <header className="flex items-center justify-between gap-3 border-b border-slate-100 px-4 py-3">
              <h2 className="truncate font-semibold text-clinic-ink">{documentPreview.fileName}</h2>
              <button type="button" aria-label="Close preview" onClick={() => setDocumentPreview(null)} className="rounded-lg p-2 text-slate-500 hover:bg-slate-100"><X size={18} /></button>
            </header>
            {documentPreview.contentType === 'application/pdf'
              ? <iframe title={documentPreview.fileName} src={documentPreview.url} className="min-h-[65vh] w-full flex-1" />
              : <div className="grid min-h-[65vh] place-items-center overflow-auto bg-slate-100 p-3"><img src={documentPreview.url} alt={documentPreview.fileName} className="max-h-[70vh] max-w-full object-contain" /></div>}
          </section>
        </div>
      )}
    </main>
  );
}

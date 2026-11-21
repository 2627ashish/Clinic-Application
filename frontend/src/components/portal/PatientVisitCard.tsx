import { Download, Eye, FileText, Receipt } from 'lucide-react';
import type { PatientVisitSummary } from '../../api/client';
import type { PatientDocumentView } from '../../types';

interface PatientVisitCardProps {
  visit: PatientVisitSummary;
  handwrittenPrescription?: PatientDocumentView;
  handwrittenPreviewUrl?: string;
  onViewPrescription: () => void;
  onViewBill: () => void;
  onViewHandwrittenPrescription: () => void;
  onDownloadHandwrittenPrescription: () => void;
}

export default function PatientVisitCard({
  visit,
  handwrittenPrescription,
  handwrittenPreviewUrl,
  onViewPrescription,
  onViewBill,
  onViewHandwrittenPrescription,
  onDownloadHandwrittenPrescription,
}: PatientVisitCardProps) {
  return (
    <article className="rounded-2xl border border-slate-100 bg-white p-4">
      <p className="text-xs font-semibold uppercase tracking-wide text-clinic-green">
        {new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(visit.visitDate))}
      </p>
      <p className="mt-2 text-sm text-clinic-ink"><strong>Diagnosis:</strong> {visit.diagnosis}</p>
      <p className="mt-1 text-sm text-clinic-ink"><strong>Procedure:</strong> {visit.procedurePerformed}</p>
      {visit.medicines.length > 0 && (
        <div className="mt-3 rounded-xl bg-clinic-paper p-3">
          <p className="text-sm font-bold text-clinic-ink">Prescription</p>
          <ul className="mt-2 space-y-2 text-sm text-slate-700">
            {visit.medicines.map((medicine, index) => (
              <li key={`${medicine.name}-${index}`}>
                <strong>{medicine.name}</strong> · {medicine.dosage} · {medicine.duration}
                <span className="block text-xs text-slate-500">{medicine.instructions}</span>
              </li>
            ))}
          </ul>
        </div>
      )}
      {visit.aftercareGuidelines && <p className="mt-2 text-sm text-clinic-ink"><strong>Aftercare:</strong> {visit.aftercareGuidelines}</p>}
      <p className="mt-2 text-xs text-slate-500">Dr. B.S. Chauhan</p>
      <div className="mt-4 grid gap-2 sm:grid-cols-2">
        <button type="button" onClick={onViewPrescription}
          className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-emerald-50 px-3 text-sm font-bold text-emerald-800 transition hover:bg-emerald-100">
          <FileText className="h-4 w-4 text-emerald-600" aria-hidden="true" />
          View Prescription PDF
        </button>
        <button type="button" onClick={onViewBill}
          className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-blue-50 px-3 text-sm font-bold text-blue-800 transition hover:bg-blue-100">
          <Receipt className="h-4 w-4 text-blue-600" aria-hidden="true" />
          View Outpatient Bill &amp; Invoice
        </button>
      </div>
      {handwrittenPrescription && (
        <div className="mt-4 rounded-xl border border-slate-100 bg-slate-50 p-3">
          <div className="flex items-center gap-3">
            {handwrittenPreviewUrl && (
              <img src={handwrittenPreviewUrl} alt="Preview of the doctor's handwritten prescription"
                className="h-16 w-16 rounded-lg bg-white object-contain" />
            )}
            <div>
              <p className="text-sm font-bold text-clinic-ink">Doctor&apos;s Handwritten Rx</p>
              <p className="text-xs text-slate-500">{handwrittenPrescription.fileName}</p>
            </div>
          </div>
          <div className="mt-3 grid gap-2 sm:grid-cols-2">
            <button type="button" onClick={onViewHandwrittenPrescription}
              className="inline-flex min-h-10 items-center justify-center gap-2 rounded-lg border border-emerald-200 bg-white px-3 text-sm font-semibold text-emerald-800 shadow-sm transition hover:bg-emerald-50">
              <Eye size={16} aria-hidden="true" /> View Fullscreen
            </button>
            <button type="button" onClick={onDownloadHandwrittenPrescription}
              aria-label="Download doctor's handwritten prescription"
              className="inline-flex min-h-10 items-center justify-center gap-2 rounded-lg border border-emerald-700 bg-emerald-700 px-3 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-800">
              <Download size={16} aria-hidden="true" /> Prescription
            </button>
          </div>
        </div>
      )}
    </article>
  );
}

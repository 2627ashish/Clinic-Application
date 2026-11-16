import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import axios from 'axios';
import { ChevronLeft, ChevronRight, ClipboardPlus, Paperclip, PencilLine, X } from 'lucide-react';
import { completeVisit, getPatientVisitHistory, uploadAppointmentDocument } from '../api/client';
import type { AppointmentView, BillItem, ConsultationCompleteRequest, PatientDocumentCategory, PrescriptionItem, TreatmentAndBill } from '../types';
import AftercareEditor from './consultation/AftercareEditor';
import BillEditor from './consultation/BillEditor';
import DigitalApplePencilPad from './consultation/DigitalApplePencilPad';
import MedicineEditor from './consultation/MedicineEditor';

interface ConsultationModalProps {
  appointment: AppointmentView;
  onClose: () => void;
  onCompleted: () => void;
}

const fieldClass = 'mt-1.5 w-full rounded-xl border border-slate-200 px-3.5 py-3 text-sm text-clinic-ink placeholder:text-slate-400';

function hasIncompleteMedicine(items: PrescriptionItem[]): boolean {
  return items.some((item) => !item.name.trim() || !item.dosage || !item.duration || !item.instructions);
}

function getSaveError(error: unknown): string {
  if (!axios.isAxiosError(error)) {
    return 'The visit could not be saved. Please try again.';
  }
  const responseData: unknown = error.response?.data;
  if (typeof responseData === 'string' && responseData.trim()) return responseData;
  if (typeof responseData === 'object' && responseData !== null && 'message' in responseData) {
    const message = responseData.message;
    if (typeof message === 'string' && message.trim()) return message;
  }
  return error.response
    ? `The server rejected the visit (HTTP ${error.response.status}). Check the details and try again.`
    : 'The server could not be reached. Check your connection and try again.';
}

export default function ConsultationModal({ appointment, onClose, onCompleted }: ConsultationModalProps) {
  const [diagnosis, setDiagnosis] = useState('');
  const [procedure, setProcedure] = useState('');
  const [medicines, setMedicines] = useState<PrescriptionItem[]>([]);
  const [aftercare, setAftercare] = useState('');
  const [billItems, setBillItems] = useState<BillItem[]>([{ description: 'Consultation', amount: 500 }]);
  const [paidAmount, setPaidAmount] = useState('0');
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [prescriptionMode, setPrescriptionMode] = useState<'typed' | 'handwritten'>('typed');
  const [showHandwrittenPad, setShowHandwrittenPad] = useState(false);
  const [visitHistory, setVisitHistory] = useState<TreatmentAndBill[]>([]);
  const [historyIndex, setHistoryIndex] = useState(0);
  const [historyError, setHistoryError] = useState('');
  const [documentFile, setDocumentFile] = useState<File | null>(null);
  const [documentCategory, setDocumentCategory] = useState<Exclude<PatientDocumentCategory, 'HANDWRITTEN_RX'>>('XRAY_RVG');
  const [uploadingDocument, setUploadingDocument] = useState(false);
  const [documentNotice, setDocumentNotice] = useState('');

  useEffect(() => {
    let active = true;
    void getPatientVisitHistory(appointment.id)
      .then((history) => {
        if (active) setVisitHistory(history);
      })
      .catch(() => {
        if (active) setHistoryError('Previous visits could not be loaded.');
      });
    return () => { active = false; };
  }, [appointment.id]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (billItems.length === 0) {
      setError('Add at least one bill item before saving the visit.');
      return;
    }
    if (billItems.some((item) => !item.description.trim() || !Number.isFinite(item.amount) || item.amount < 0)) {
      setError('Enter a description and a valid non-negative cost for every bill item.');
      return;
    }
    if (prescriptionMode === 'typed' && hasIncompleteMedicine(medicines)) {
      setError('Complete every medicine row or remove it. Name, dosage, duration, and instructions are required.');
      return;
    }
    const request: ConsultationCompleteRequest = {
      symptomsAndDiagnosis: diagnosis,
      procedurePerformed: procedure,
      medicines: prescriptionMode === 'typed' ? medicines : [],
      aftercareGuidelines: aftercare,
      billItems,
      paidAmount: Number(paidAmount),
    };
    if (!Number.isFinite(request.paidAmount) || request.paidAmount < 0) {
      setError('Enter a valid amount paid.');
      return;
    }
    const billTotal = billItems.reduce((total, item) => total + item.amount, 0);
    if (request.paidAmount > billTotal) {
      setError('Amount paid cannot exceed the bill total.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      await completeVisit(appointment.id, request);
      onCompleted();
    } catch (submitError: unknown) {
      setError(getSaveError(submitError));
    } finally {
      setSubmitting(false);
    }
  }

  async function uploadDocument() {
    if (!documentFile) return;
    setUploadingDocument(true);
    setDocumentNotice('');
    setError('');
    try {
      await uploadAppointmentDocument(appointment.id, documentFile, documentCategory);
      setDocumentNotice(`${documentFile.name} uploaded.`);
      setDocumentFile(null);
    } catch (uploadError: unknown) {
      setError(getSaveError(uploadError));
    } finally {
      setUploadingDocument(false);
    }
  }

  const currentHistory = visitHistory[historyIndex];

  return (
    <div className="modal-backdrop fixed inset-0 z-50 grid place-items-center overflow-y-auto bg-clinic-ink/45 p-4">
      <section role="dialog" aria-modal="true" aria-labelledby="consultation-title" className="my-auto w-full max-w-2xl rounded-3xl bg-white p-6 shadow-2xl sm:p-8">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <span className="grid h-11 w-11 place-items-center rounded-2xl bg-clinic-mint text-clinic-green"><ClipboardPlus size={22} /></span>
            <div>
              <h2 id="consultation-title" className="text-xl font-bold text-clinic-ink">Consultation &amp; bill</h2>
              <p className="text-sm text-slate-500">{appointment.patientName} · {appointment.phoneNumber}</p>
            </div>
          </div>
          <button aria-label="Close dialog" onClick={onClose} className="rounded-full p-2 text-slate-400 hover:bg-slate-100"><X size={19} /></button>
        </div>
        {visitHistory.length > 0 && (
          <section className="mt-5 rounded-2xl border border-slate-100 bg-clinic-paper p-4" aria-label="Previous prescription pages">
            <div className="flex items-center justify-between gap-3">
              <div><p className="text-xs font-bold uppercase tracking-wide text-clinic-green">Patient visit folder</p><p className="mt-1 text-sm font-semibold text-clinic-ink">Visit {visitHistory.length - historyIndex} of {visitHistory.length}</p></div>
              <div className="flex gap-2">
                <button type="button" aria-label="Newer visit" disabled={historyIndex === 0} onClick={() => setHistoryIndex((index) => Math.max(index - 1, 0))} className="rounded-lg border border-slate-200 bg-white p-2 disabled:opacity-40"><ChevronLeft size={17} /></button>
                <button type="button" aria-label="Older visit" disabled={historyIndex === visitHistory.length - 1} onClick={() => setHistoryIndex((index) => Math.min(index + 1, visitHistory.length - 1))} className="rounded-lg border border-slate-200 bg-white p-2 disabled:opacity-40"><ChevronRight size={17} /></button>
              </div>
            </div>
            {currentHistory && <div className="mt-3 grid gap-2 text-sm sm:grid-cols-2"><p><strong>Date:</strong> {new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(currentHistory.createdAt))}</p><p><strong>Diagnosis:</strong> {currentHistory.symptomsAndDiagnosis}</p><p className="sm:col-span-2"><strong>Procedure:</strong> {currentHistory.procedurePerformed}</p></div>}
          </section>
        )}
        {historyError && <p role="status" className="mt-3 text-xs text-amber-700">{historyError}</p>}
        <div className="mt-5 flex flex-wrap gap-2" role="tablist" aria-label="Prescription entry mode">
          <button type="button" role="tab" aria-selected={prescriptionMode === 'typed'} onClick={() => setPrescriptionMode('typed')}
            className={`rounded-lg px-4 py-2 text-sm font-semibold ${prescriptionMode === 'typed' ? 'bg-clinic-green text-white' : 'bg-slate-100 text-slate-600'}`}>⌨️ Type Prescription</button>
          <button type="button" role="tab" aria-selected={prescriptionMode === 'handwritten'} onClick={() => setPrescriptionMode('handwritten')}
            className={`rounded-lg px-4 py-2 text-sm font-semibold ${prescriptionMode === 'handwritten' ? 'bg-clinic-green text-white' : 'bg-slate-100 text-slate-600'}`}>✏️ Apple Pencil Handwritten Pad</button>
        </div>
        <form onSubmit={submit} className="mt-6 grid gap-4 sm:grid-cols-2">
          <label className="text-sm font-semibold text-clinic-ink">
            Symptoms &amp; diagnosis
            <textarea required rows={3} value={diagnosis} onChange={(event) => setDiagnosis(event.target.value)} className={fieldClass} placeholder="Clinical findings and diagnosis" />
          </label>
          <label className="text-sm font-semibold text-clinic-ink">
            Procedure performed
            <textarea required rows={3} value={procedure} onChange={(event) => setProcedure(event.target.value)} className={fieldClass} placeholder="Treatment provided today" />
          </label>
          {prescriptionMode === 'typed' ? <MedicineEditor medicines={medicines} onChange={setMedicines} /> : (
            <button type="button" onClick={() => setShowHandwrittenPad(true)} className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl border border-dashed border-clinic-green bg-clinic-mint px-4 font-semibold text-clinic-green sm:col-span-2">
              <PencilLine size={18} />Open Digital Prescription Pad
            </button>
          )}
          <AftercareEditor value={aftercare} onChange={setAftercare} />
          <BillEditor items={billItems} paidAmount={paidAmount} onItemsChange={setBillItems} onPaidAmountChange={setPaidAmount} />
          {error && <p role="alert" className="text-sm text-red-600 sm:col-span-2">{error}</p>}
          <button disabled={submitting} className="rounded-xl bg-clinic-green px-5 py-3 font-semibold text-white hover:bg-clinic-ink disabled:opacity-60 sm:col-span-2">
            {submitting ? 'Saving visit…' : 'Save visit & send summary'}
          </button>
        </form>
        <section className="mt-5 rounded-2xl border border-slate-100 p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div><h3 className="font-semibold text-clinic-ink">📎 Upload X-Ray / Docs</h3><p className="mt-1 text-xs text-slate-500">PDF, JPEG, PNG, or WebP · up to 8 MB</p></div>
            <select aria-label="Document category" value={documentCategory} onChange={(event) => setDocumentCategory(event.target.value as Exclude<PatientDocumentCategory, 'HANDWRITTEN_RX'>)} className="rounded-lg border border-slate-200 px-3 py-2 text-sm">
              <option value="XRAY_RVG">X-Ray / RVG</option><option value="EXTERNAL_PRESCRIPTION">External Prescription</option><option value="LAB_REPORT">Lab Report</option><option value="OTHER">Other</option>
            </select>
          </div>
          <label onDragOver={(event) => event.preventDefault()} onDrop={(event) => { event.preventDefault(); setDocumentFile(event.dataTransfer.files[0] ?? null); }}
            className="mt-3 flex min-h-20 cursor-pointer items-center justify-center gap-2 rounded-xl border border-dashed border-slate-300 bg-clinic-paper px-4 text-center text-sm text-slate-600">
            <Paperclip size={17} />{documentFile ? documentFile.name : 'Drop a file here or choose a document'}
            <input type="file" accept=".pdf,.jpg,.jpeg,.png,.webp" className="sr-only" onChange={(event) => setDocumentFile(event.target.files?.[0] ?? null)} />
          </label>
          <div className="mt-3 flex items-center justify-between gap-3">
            {documentNotice && <p role="status" className="text-xs font-medium text-emerald-700">{documentNotice}</p>}
            <button type="button" disabled={!documentFile || uploadingDocument} onClick={() => void uploadDocument()} className="ml-auto min-h-10 rounded-lg bg-clinic-green px-4 text-sm font-semibold text-white disabled:opacity-50">{uploadingDocument ? 'Uploading…' : 'Upload document'}</button>
          </div>
        </section>
      </section>
      {showHandwrittenPad && <DigitalApplePencilPad appointmentId={appointment.id} patientName={appointment.patientName} onClose={() => setShowHandwrittenPad(false)} onSaved={() => setDocumentNotice('Handwritten prescription saved to the patient visit timeline.')} />}
    </div>
  );
}

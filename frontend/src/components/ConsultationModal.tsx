import { useState } from 'react';
import type { FormEvent } from 'react';
import axios from 'axios';
import { ClipboardPlus, X } from 'lucide-react';
import { completeVisit } from '../api/client';
import type { AppointmentView, BillItem, ConsultationCompleteRequest, PrescriptionItem } from '../types';
import AftercareEditor from './consultation/AftercareEditor';
import BillEditor from './consultation/BillEditor';
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
    if (hasIncompleteMedicine(medicines)) {
      setError('Complete every medicine row or remove it. Name, dosage, duration, and instructions are required.');
      return;
    }
    const request: ConsultationCompleteRequest = {
      symptomsAndDiagnosis: diagnosis,
      procedurePerformed: procedure,
      medicines,
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
        <form onSubmit={submit} className="mt-6 grid gap-4 sm:grid-cols-2">
          <label className="text-sm font-semibold text-clinic-ink">
            Symptoms &amp; diagnosis
            <textarea required rows={3} value={diagnosis} onChange={(event) => setDiagnosis(event.target.value)} className={fieldClass} placeholder="Clinical findings and diagnosis" />
          </label>
          <label className="text-sm font-semibold text-clinic-ink">
            Procedure performed
            <textarea required rows={3} value={procedure} onChange={(event) => setProcedure(event.target.value)} className={fieldClass} placeholder="Treatment provided today" />
          </label>
          <MedicineEditor medicines={medicines} onChange={setMedicines} />
          <AftercareEditor value={aftercare} onChange={setAftercare} />
          <BillEditor items={billItems} paidAmount={paidAmount} onItemsChange={setBillItems} onPaidAmountChange={setPaidAmount} />
          {error && <p role="alert" className="text-sm text-red-600 sm:col-span-2">{error}</p>}
          <button disabled={submitting} className="rounded-xl bg-clinic-green px-5 py-3 font-semibold text-white hover:bg-clinic-ink disabled:opacity-60 sm:col-span-2">
            {submitting ? 'Saving visit…' : 'Save visit & send summary'}
          </button>
        </form>
      </section>
    </div>
  );
}

import { Plus, Trash2 } from 'lucide-react';
import type { PrescriptionItem } from '../../types';

const dosageOptions = [
  '1-0-1 (Twice Daily / सुबह-शाम)',
  '1-1-1 (Thrice Daily / सुबह-दोपहर-रात)',
  '1-0-0 (Morning only / सिर्फ सुबह)',
  '0-0-1 (Night only / सिर्फ रात)',
  'SOS (Only if severe pain / दर्द होने पर)',
];
const durationOptions = ['1 Day', '2 Days', '3 Days', '4 Days', '5 Days', '6 Days', '7 Days', '15 Days', '30 Days'];
const instructionOptions = ['After Food (खाने के बाद)', 'Before Food (खाने से पहले)'];
const medicineSuggestions = [
  'Flexon-MR',
  'Taxim-O',
  'Zerodol-SP',
  'Amoxicillin',
  'Ketorol-DT',
  'Pantocid',
];

interface MedicineEditorProps {
  medicines: PrescriptionItem[];
  onChange: (items: PrescriptionItem[]) => void;
}

function updateMedicine(
  medicines: PrescriptionItem[],
  onChange: MedicineEditorProps['onChange'],
  index: number,
  field: keyof PrescriptionItem,
  value: string,
) {
  onChange(medicines.map((medicine, row) => row === index ? { ...medicine, [field]: value } : medicine));
}

export default function MedicineEditor({ medicines, onChange }: MedicineEditorProps) {
  return (
    <section className="sm:col-span-2" aria-labelledby="medicine-heading">
      <div className="flex items-center justify-between gap-3">
        <h3 id="medicine-heading" className="text-sm font-semibold text-clinic-ink">Prescription medicines</h3>
        <button
          type="button"
          onClick={() => onChange([...medicines, { name: '', dosage: '', duration: '', instructions: '' }])}
          className="inline-flex items-center gap-1 rounded-lg bg-clinic-mint px-3 py-2 text-xs font-bold text-clinic-green"
        >
          <Plus size={15} /> Add medicine
        </button>
      </div>
      <datalist id="medicine-suggestions">
        {medicineSuggestions.map((medicine) => <option key={medicine} value={medicine} />)}
      </datalist>
      <div className="mt-2 overflow-x-auto rounded-xl border border-slate-200">
        <table className="w-full min-w-[700px] text-left text-xs">
          <thead className="bg-clinic-paper text-slate-600">
            <tr>
              <th className="p-2.5">Medicine name</th>
              <th className="p-2.5">Dosage</th>
              <th className="p-2.5">Duration</th>
              <th className="p-2.5">Instructions</th>
              <th className="p-2.5"><span className="sr-only">Remove</span></th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {medicines.length === 0 ? (
              <tr><td colSpan={5} className="p-3 text-center text-slate-500">No medicines prescribed.</td></tr>
            ) : medicines.map((medicine, index) => (
              <tr key={index}>
                <td className="p-2">
                  <input aria-label={`Medicine ${index + 1} name`} list="medicine-suggestions" value={medicine.name}
                    onChange={(event) => updateMedicine(medicines, onChange, index, 'name', event.target.value)}
                    className="w-full rounded-lg border border-slate-200 px-2 py-2" />
                </td>
                <td className="p-2">
                  <select aria-label={`Medicine ${index + 1} dosage`} value={medicine.dosage}
                    onChange={(event) => updateMedicine(medicines, onChange, index, 'dosage', event.target.value)}
                    className="w-full rounded-lg border border-slate-200 px-2 py-2">
                    <option value="">Select</option>
                    {dosageOptions.map((option) => <option key={option}>{option}</option>)}
                  </select>
                </td>
                <td className="p-2">
                  <select aria-label={`Medicine ${index + 1} duration`} value={medicine.duration}
                    onChange={(event) => updateMedicine(medicines, onChange, index, 'duration', event.target.value)}
                    className="w-full rounded-lg border border-slate-200 px-2 py-2">
                    <option value="">Select</option>
                    {durationOptions.map((option) => <option key={option}>{option}</option>)}
                  </select>
                </td>
                <td className="p-2">
                  <select aria-label={`Medicine ${index + 1} instructions`} value={medicine.instructions}
                    onChange={(event) => updateMedicine(medicines, onChange, index, 'instructions', event.target.value)}
                    className="w-full rounded-lg border border-slate-200 px-2 py-2">
                    <option value="">Select</option>
                    {instructionOptions.map((option) => <option key={option}>{option}</option>)}
                  </select>
                </td>
                <td className="p-2">
                  <button type="button" aria-label={`Remove medicine ${index + 1}`}
                    onClick={() => onChange(medicines.filter((_, row) => row !== index))}
                    className="rounded-lg p-2 text-rose-600 hover:bg-rose-50"><Trash2 size={16} /></button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

import type { Dispatch, SetStateAction } from 'react';
import type { MedicalDeclaration } from '../../types/appointment';

interface MedicalDeclarationFieldsProps {
  gender: 'Male' | 'Female' | 'Other' | '';
  age: number | null;
  declaration: MedicalDeclaration;
  setDeclaration: Dispatch<SetStateAction<MedicalDeclaration>>;
  isPregnant: boolean | null;
  setIsPregnant: Dispatch<SetStateAction<boolean | null>>;
  isBreastfeeding: boolean | null;
  setIsBreastfeeding: Dispatch<SetStateAction<boolean | null>>;
}

const checklist: [keyof Pick<MedicalDeclaration,
  'hasChronicIllness' | 'takingLongTermMedication' | 'lignocaineConsent' | 'consentToTreatment'
  | 'adherenceConsent' | 'hadPreviousDentalTreatment' | 'rctCommitment'>, string][] = [
    ['hasChronicIllness', 'Any chronic illness? / कोई पुरानी बीमारी'],
    ['takingLongTermMedication', 'Long-term medication? / लंबे समय से दवाई'],
    ['lignocaineConsent', 'Consent for lignocaine anesthesia / एनेस्थीसिया सहमति'],
    ['consentToTreatment', 'Consent to treatment by Dr. B.S. Chauhan'],
    ['adherenceConsent', 'Agree to follow care instructions / नियम पालन सहमति'],
    ['hadPreviousDentalTreatment', 'Previous dental treatment? / पहले दांत का इलाज'],
    ['rctCommitment', 'Agree to attend RCT visits on time / RCT समय सहमति'],
  ];

export default function MedicalDeclarationFields({
  gender,
  age,
  declaration,
  setDeclaration,
  isPregnant,
  setIsPregnant,
  isBreastfeeding,
  setIsBreastfeeding,
}: MedicalDeclarationFieldsProps) {
  const pregnancyQuestions = [
    { key: 'pregnant', label: 'Are you pregnant? / गर्भवती', value: isPregnant, setValue: setIsPregnant },
    { key: 'breastfeeding', label: 'Are you breastfeeding? / स्तनपान', value: isBreastfeeding, setValue: setIsBreastfeeding },
  ];

  return (
    <>
      <fieldset className="rounded-2xl border border-slate-200 p-4">
        <legend className="px-1 text-sm font-bold text-clinic-ink">Medical declaration &amp; consent / चिकित्सा घोषणा और सहमति</legend>
        <div className="grid gap-2 sm:grid-cols-2">
          {checklist.map(([key, label]) => (
            <label key={key} className="flex items-start gap-2 text-xs leading-5 text-slate-700">
              <input type="checkbox" checked={declaration[key]} onChange={(event) =>
                setDeclaration((current) => ({ ...current, [key]: event.target.checked }))}
              className="mt-1 accent-clinic-green" />
              {label}
            </label>
          ))}
        </div>
        <div className="mt-3 grid gap-3 sm:grid-cols-2">
          <label className="text-xs font-semibold text-slate-600">Illness details
            <input value={declaration.illnessDetails}
              onChange={(event) => setDeclaration((current) => ({ ...current, illnessDetails: event.target.value }))}
              maxLength={500} className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 font-normal" />
          </label>
          <label className="text-xs font-semibold text-slate-600">Medication details
            <input value={declaration.medicationDetails}
              onChange={(event) => setDeclaration((current) => ({ ...current, medicationDetails: event.target.value }))}
              maxLength={500} className="mt-1 w-full rounded-lg border border-slate-200 px-3 py-2 font-normal" />
          </label>
        </div>
      </fieldset>
      {gender === 'Female' && (age ?? 0) >= 18 && (
        <div className="grid gap-4 sm:grid-cols-2">
          {pregnancyQuestions.map(({ key, label, value, setValue }) => (
            <fieldset key={key} className="text-sm font-semibold text-clinic-ink">
              <legend>{label}</legend>
              <div className="mt-2 flex gap-2">
                {([[true, 'Yes / हाँ'], [false, 'No / नहीं']] as const).map(([answer, answerLabel]) => (
                  <button key={answerLabel} type="button" aria-pressed={value === answer}
                    onClick={() => setValue(answer)}
                    className={`min-h-11 flex-1 rounded-full border px-3 text-sm font-semibold ${value === answer ? 'border-clinic-green bg-clinic-mint text-clinic-green' : 'border-slate-200 text-slate-600'}`}>
                    {answerLabel}
                  </button>
                ))}
              </div>
            </fieldset>
          ))}
        </div>
      )}
    </>
  );
}

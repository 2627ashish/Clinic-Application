import { useState } from 'react';
import type { FormEvent } from 'react';
import { MessageCircle, X } from 'lucide-react';
import { getFirstAidGuidance, type GuidanceLanguage } from './firstAidGuidance';
import type { ConsultationType } from '../../types/appointment';

interface AiSelfHelpReceptionistModalProps {
  onClose: () => void;
  onBook: (symptoms: string, age: number, gender: 'Male' | 'Female' | 'Other', consultationType: ConsultationType) => void;
}

const greeting: Record<GuidanceLanguage, string> = {
  en: "Hello! I am Maya, Dr. Chauhan's virtual receptionist. What dental concern are you facing today?",
  hi: 'नमस्ते! मैं माया, डॉ. चौहान की वर्चुअल रिसेप्शनिस्ट हूँ। आज आपको दाँत या मसूड़ों से जुड़ी क्या परेशानी है?',
};

const bookingPrompt: Record<GuidanceLanguage, string> = {
  en: 'Would you like to schedule a consultation with Dr. Chauhan?',
  hi: 'क्या आप डॉ. चौहान से चेकअप के लिए अपॉइंटमेंट बुक करना चाहते हैं?',
};

const quickSymptoms: Record<GuidanceLanguage, string[]> = {
  en: ['Severe toothache', 'Bleeding gums', 'Teeth sensitivity', 'Facial swelling'],
  hi: ['दाँत दर्द', 'मसूड़ों से खून', 'दाँतों में झनझनाहट', 'चेहरे में सूजन'],
};

export default function AiSelfHelpReceptionistModal({ onClose, onBook }: AiSelfHelpReceptionistModalProps) {
  const [language, setLanguage] = useState<GuidanceLanguage>('en');
  const [symptoms, setSymptoms] = useState('');
  const [age, setAge] = useState('');
  const [gender, setGender] = useState<'Male' | 'Female' | 'Other' | ''>('');
  const [consultationType, setConsultationType] = useState<ConsultationType>('IN_CLINIC');
  const [guidance, setGuidance] = useState<{ title: string; advice: string } | null>(null);

  function getAdvice(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const result = getFirstAidGuidance(symptoms.trim(), language);
    setGuidance(result);
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-clinic-ink/60 p-3 backdrop-blur-sm sm:p-6" role="presentation">
      <section role="dialog" aria-modal="true" aria-labelledby="maya-title" className="max-h-[92vh] w-full max-w-xl overflow-y-auto rounded-3xl bg-white p-5 shadow-2xl sm:p-7">
        <header className="flex items-start justify-between gap-3">
          <div>
            <p className="text-sm font-bold uppercase tracking-wider text-clinic-green">Maya · Chauhan Dental Clinic</p>
            <h2 id="maya-title" className="mt-1 text-2xl font-bold text-clinic-ink">Dental Self-Help &amp; Triage</h2>
          </div>
          <button type="button" onClick={onClose} aria-label="Close Maya assistant" className="rounded-full p-2 text-slate-500 hover:bg-slate-100"><X size={20} /></button>
        </header>

        <div className="mt-5 flex flex-wrap items-center justify-between gap-3">
          <div className="inline-flex rounded-full bg-clinic-paper p-1" aria-label="Select language">
            <button type="button" aria-pressed={language === 'en'} onClick={() => setLanguage('en')} className={`rounded-full px-3 py-2 text-sm font-semibold ${language === 'en' ? 'bg-white text-clinic-green shadow-sm' : 'text-slate-500'}`}>🇬🇧 English</button>
            <button type="button" aria-pressed={language === 'hi'} onClick={() => setLanguage('hi')} className={`rounded-full px-3 py-2 text-sm font-semibold ${language === 'hi' ? 'bg-white text-clinic-green shadow-sm' : 'text-slate-500'}`}>🇮🇳 हिंदी</button>
          </div>
        </div>

        <p className="mt-5 rounded-2xl bg-clinic-mint/60 p-4 text-sm leading-6 text-clinic-ink" aria-live="polite">{greeting[language]}</p>
        <form onSubmit={getAdvice} className="mt-5 space-y-4">
          <div className="flex flex-wrap gap-2">
            {quickSymptoms[language].map((symptom) => (
              <button
                key={symptom}
                type="button"
                onClick={() => setSymptoms(symptom)}
                className="rounded-full border border-clinic-green/20 px-3 py-2 text-xs font-semibold text-clinic-green hover:bg-clinic-mint"
              >
                {symptom}
              </button>
            ))}
          </div>
          <label htmlFor="maya-symptoms" className="block text-sm font-semibold text-clinic-ink">
            {language === 'en' ? 'Tell Maya what is bothering you' : 'माया को अपनी परेशानी बताएं'}
            <textarea
              id="maya-symptoms"
              value={symptoms}
              onChange={(event) => setSymptoms(event.target.value)}
              rows={3}
              maxLength={1000}
              className="mt-1.5 w-full rounded-xl border border-slate-200 px-3.5 py-3 font-normal outline-none focus:border-clinic-green focus:ring-2 focus:ring-clinic-green/15"
              placeholder={language === 'en' ? 'Type your dental concern…' : 'दाँत की परेशानी लिखें…'}
            />
          </label>
          <button type="submit" disabled={!symptoms.trim()} className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-clinic-green px-5 py-3 font-semibold text-white hover:bg-clinic-ink disabled:cursor-not-allowed disabled:opacity-50">
            <MessageCircle size={18} /> {language === 'en' ? 'Ask Maya for guidance' : 'माया से सुझाव पूछें'}
          </button>
        </form>

        {guidance && (
          <section className="mt-5 rounded-2xl border border-clinic-green/15 bg-clinic-paper p-4" aria-live="polite">
            <h3 className="font-bold text-clinic-ink">{guidance.title}</h3>
            <p className="mt-2 text-sm leading-6 text-slate-700">{guidance.advice}</p>
            <p className="mt-3 text-sm font-semibold text-clinic-green">{bookingPrompt[language]}</p>
            <fieldset className="mt-3">
              <legend className="text-sm font-semibold text-clinic-ink">{language === 'en' ? 'Consultation mode' : 'परामर्श का तरीका'}</legend>
              <div className="mt-2 grid gap-2 sm:grid-cols-2">
                {([
                  ['IN_CLINIC', language === 'en' ? '🏥 In-Clinic Visit' : '🏥 क्लिनिक में'],
                  ['ONLINE', language === 'en' ? '💻 Online via WhatsApp' : '💻 व्हाट्सऐप पर ऑनलाइन'],
                ] as const).map(([type, label]) => (
                  <button key={type} type="button" aria-pressed={consultationType === type}
                    onClick={() => setConsultationType(type)}
                    className={`min-h-11 rounded-xl border px-3 py-2 text-sm font-semibold ${consultationType === type ? 'border-clinic-green bg-clinic-mint text-clinic-green' : 'border-slate-200 text-slate-600'}`}>
                    {label}
                  </button>
                ))}
              </div>
              {consultationType === 'ONLINE' && <p className="mt-2 text-xs leading-5 text-slate-600">{language === 'en' ? 'Dr. B.S. Chauhan will connect with you via WhatsApp Video Call at your booked time.' : 'डॉ. चौहान बुक किए गए समय पर व्हाट्सऐप वीडियो कॉल करेंगे।'}</p>}
            </fieldset>
            <label className="mt-3 block text-sm font-semibold text-clinic-ink">
              {language === 'en' ? 'Age' : 'आयु'}
              <input type="number" inputMode="numeric" min="1" max="120" value={age}
                onChange={(event) => setAge(event.target.value)}
                className="mt-1.5 w-full rounded-xl border border-slate-200 px-3 py-2.5 font-normal" />
            </label>
            <fieldset className="mt-3">
              <legend className="text-sm font-semibold text-clinic-ink">{language === 'en' ? 'Gender' : 'लिंग'}</legend>
              <div className="mt-2 flex gap-2">
                {(['Male', 'Female', 'Other'] as const).map((value) => (
                  <button key={value} type="button" aria-pressed={gender === value}
                    onClick={() => setGender(value)}
                    className={`min-h-11 flex-1 rounded-full border px-3 text-sm font-semibold ${gender === value ? 'border-clinic-green bg-clinic-mint text-clinic-green' : 'border-slate-200 text-slate-600'}`}>
                    {value}
                  </button>
                ))}
              </div>
            </fieldset>
            <button type="button" disabled={!age || Number(age) < 1 || Number(age) > 120 || !gender}
              onClick={() => gender && onBook(symptoms.trim(), Number(age), gender, consultationType)}
              className="mt-4 w-full rounded-xl bg-clinic-green px-5 py-3 font-semibold text-white hover:bg-clinic-ink disabled:cursor-not-allowed disabled:opacity-50">
              {language === 'en' ? 'Book with Dr. Chauhan' : 'डॉ. चौहान के साथ बुक करें'}
            </button>
          </section>
        )}
        <p className="mt-4 text-xs leading-5 text-slate-500">
          {language === 'en' ? 'Maya shares general first-aid information, not a diagnosis. Seek urgent help for severe symptoms.' : 'माया सामान्य प्राथमिक जानकारी देती हैं, निदान नहीं। गंभीर लक्षणों में तुरंत सहायता लें।'}
        </p>
      </section>
    </div>
  );
}

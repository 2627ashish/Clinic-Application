interface AftercareEditorProps {
  value: string;
  onChange: (value: string) => void;
}

const suggestions = [
  'Avoid hot food for 24h / 24 घंटे तक गर्म खाना न खाएं',
  'Rinse with warm salt water after 24h / गुनगुने नमक के पानी से कुल्ला करें',
  'Soft diet only / केवल नरम आहार लें',
  'Do not spit or use straw / थूकें नहीं और स्ट्रॉ का प्रयोग न करें',
];

export default function AftercareEditor({ value, onChange }: AftercareEditorProps) {
  function toggleSuggestion(suggestion: string) {
    const selected = value.split('\n').filter(Boolean);
    onChange(selected.includes(suggestion)
      ? selected.filter((item) => item !== suggestion).join('\n')
      : [...selected, suggestion].join('\n'));
  }

  return (
    <section className="text-sm font-semibold text-clinic-ink">
      <label htmlFor="aftercare-guidelines">Aftercare guidelines</label>
      <div className="mt-2 flex flex-wrap gap-2">
        {suggestions.map((suggestion) => {
          const selected = value.split('\n').includes(suggestion);
          return (
            <button key={suggestion} type="button" aria-pressed={selected} onClick={() => toggleSuggestion(suggestion)}
              className={`rounded-full border px-3 py-1.5 text-xs font-medium ${selected ? 'border-clinic-green bg-clinic-mint text-clinic-green' : 'border-slate-200 text-slate-600'}`}>
              {suggestion}
            </button>
          );
        })}
      </div>
      <textarea id="aftercare-guidelines" required rows={3} value={value}
        onChange={(event) => onChange(event.target.value)}
        className="mt-2 w-full rounded-xl border border-slate-200 px-3.5 py-3 text-sm font-normal text-clinic-ink placeholder:text-slate-400"
        placeholder="Enter custom advice / विशेष निर्देश लिखें" />
    </section>
  );
}

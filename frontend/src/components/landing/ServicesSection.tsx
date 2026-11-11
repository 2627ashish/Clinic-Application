import { Baby, Braces, HeartPulse, Smile, Sparkles } from 'lucide-react';

const services = [
  { icon: HeartPulse, title: 'Root Canal', description: 'Care focused on relieving tooth pain and preserving your natural tooth.' },
  { icon: Smile, title: 'Dental Implants', description: 'Personalized options to help restore your smile and chewing comfort.' },
  { icon: Sparkles, title: 'Teeth Whitening', description: 'Professional guidance for a brighter, confident smile.' },
  { icon: Braces, title: 'Orthodontics & Braces', description: 'Treatment options designed to help align your teeth over time.' },
  { icon: Baby, title: 'Pediatric Dentistry', description: 'Friendly dental visits and preventive care for growing smiles.' },
];

export default function ServicesSection() {
  return (
    <section className="border-y border-clinic-ink/5 bg-white py-14 sm:py-16" aria-labelledby="services-title">
      <div className="mx-auto max-w-7xl px-5 sm:px-8">
        <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
          <div>
            <p className="text-sm font-bold uppercase tracking-[0.16em] text-clinic-green">How we can help</p>
            <h2 id="services-title" className="mt-2 text-3xl font-bold tracking-tight text-clinic-ink">Dental care for every smile</h2>
          </div>
          <p className="max-w-md text-sm leading-6 text-slate-500">Tell us what you need and our clinic team can help you plan your next visit.</p>
        </div>
        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {services.map(({ icon: Icon, title, description }) => (
            <article key={title} className="rounded-2xl border border-slate-100 bg-clinic-paper/70 p-6 transition hover:-translate-y-1 hover:bg-white hover:shadow-soft">
              <span className="grid h-12 w-12 place-items-center rounded-2xl bg-white text-clinic-green shadow-sm">
                <Icon size={22} aria-hidden="true" />
              </span>
              <h3 className="mt-5 font-bold text-clinic-ink">{title}</h3>
              <p className="mt-2 text-sm leading-6 text-slate-500">{description}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

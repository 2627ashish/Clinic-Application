import { ArrowRight } from 'lucide-react';
import { DENTAL_IMAGES } from '../../constants/dentalImages';

const services = [
  {
    title: 'Root Canal Treatment (RCT)',
    badge: 'Root Canal (RCT)',
    hindi: 'रूट कैनाल',
    description: 'Preserve your natural tooth with carefully planned root canal care.',
    details: 'Root canal treatment removes infected tissue, relieves the source of infection, and helps retain your natural tooth. A protective crown may be recommended.',
    image: DENTAL_IMAGES.services.rct,
  },
  {
    title: 'Dental Implants & Crowns',
    badge: 'Implants & Crowns',
    hindi: 'डेंटल इंप्लांट',
    description: 'Restore chewing comfort with a plan tailored to your smile.',
    details: 'Implants and crowns are planned around your oral health, bone support, and bite. Your dentist will discuss suitable options and expected follow-up.',
    image: DENTAL_IMAGES.services.implants,
  },
  {
    title: 'Teeth Cleaning & Scaling',
    badge: 'Ultrasonic Scaling',
    hindi: 'स्केलिंग',
    description: 'Support healthier gums with professional cleaning and polishing.',
    details: 'Scaling removes hardened deposits that regular brushing cannot clear. Your dentist can recommend an interval based on your gum health.',
    image: DENTAL_IMAGES.services.scaling,
  },
  {
    title: 'Cosmetic & Silver Fillings',
    badge: 'Cosmetic Fillings',
    hindi: 'दाँत के रंग की फिलिंग',
    description: 'Repair cavities with a filling selected for the tooth and your needs.',
    details: 'Tooth-coloured composite or silver filling materials may be considered depending on the tooth, cavity size, and clinical assessment.',
    image: DENTAL_IMAGES.services.fillings,
  },
  {
    title: 'Orthodontic Braces',
    badge: 'Braces',
    hindi: 'ऑर्थोडॉन्टिक देखभाल',
    description: 'Explore a plan to improve tooth alignment and your bite.',
    details: 'An orthodontic assessment helps determine whether braces are suitable and how long treatment may take for your particular bite.',
    image: DENTAL_IMAGES.services.braces,
  },
  {
    title: 'Digital RVG X-Ray',
    badge: 'Digital X-Ray RVG',
    hindi: 'डिजिटल एक्स-रे',
    description: 'Digital imaging supports assessment and treatment planning.',
    details: 'A digital RVG image can help your dentist examine tooth roots and surrounding structures when clinically indicated.',
    image: DENTAL_IMAGES.services.xray,
  },
  {
    title: 'Laser Filling',
    badge: 'Laser Filling',
    hindi: 'लेजर फिलिंग',
    description: 'Consider laser-assisted care for selected tooth-coloured fillings.',
    details: 'In suitable cases, laser-assisted composite treatment may reduce the need for conventional drilling. Your dentist will explain the options after examining the tooth.',
    image: DENTAL_IMAGES.services.fillings,
  },
  {
    title: 'Impaction & Surgical Extraction',
    badge: 'Wisdom Tooth Surgery',
    hindi: 'अक्ल दाढ़ व सर्जरी',
    description: 'Get an assessment for impacted wisdom teeth or damaged roots.',
    details: 'Surgical removal is planned after an examination and imaging. Minimally invasive techniques are used when appropriate for the case.',
    image: DENTAL_IMAGES.services.extraction,
  },
];

interface ServicesSectionProps {
  onBookConsultation: (procedure: string) => void;
}

export default function ServicesSection({ onBookConsultation }: ServicesSectionProps) {
  return (
    <section className="border-y border-slate-200/70 bg-white py-14 sm:py-16" aria-labelledby="services-title">
      <div className="mx-auto max-w-7xl px-5 sm:px-8">
        <div className="flex flex-col justify-between gap-3 sm:flex-row sm:items-end">
          <div>
            <p className="text-sm font-bold uppercase tracking-[0.16em] text-emerald-700">How we can help</p>
            <h2 id="services-title" className="mt-2 text-3xl font-extrabold tracking-tight text-slate-900 sm:text-4xl">
              Dental care for every smile
            </h2>
          </div>
          <p className="max-w-md text-sm leading-6 text-slate-500">
            Explore care options and book a consultation with the clinic team to discuss what is right for you.
          </p>
        </div>

        <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {services.map((service) => (
            <article key={service.title} className="group flex flex-col overflow-hidden rounded-3xl border border-slate-200/80 bg-white shadow-sm transition duration-300 hover:-translate-y-1 hover:shadow-xl hover:shadow-slate-900/10">
              <div className="relative aspect-[3/2] overflow-hidden rounded-xl bg-emerald-50">
                <img src={service.image} alt={service.title} loading="lazy"
                  onError={(event) => {
                    event.currentTarget.onerror = null;
                    event.currentTarget.src = DENTAL_IMAGES.clinicAmbience;
                  }}
                  className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105" />
                <div className="absolute inset-0 bg-gradient-to-t from-slate-950/55 via-transparent to-transparent" aria-hidden="true" />
                <div className="absolute inset-x-3 bottom-3 flex flex-wrap items-end justify-between gap-2">
                  <span className="rounded-full border border-white/60 bg-white/90 px-3 py-1.5 text-xs font-bold text-slate-800 shadow-sm backdrop-blur">
                    {service.badge}
                  </span>
                  <span lang="hi" className="rounded-full bg-emerald-700/90 px-2.5 py-1.5 text-xs font-semibold text-white backdrop-blur">
                    {service.hindi}
                  </span>
                </div>
              </div>
              <div className="flex flex-1 flex-col p-5">
                <h3 className="text-base font-bold leading-6 text-slate-900">{service.title}</h3>
                <p className="mt-2 min-h-10 text-sm leading-5 text-slate-600">{service.description}</p>
                <div className="grid max-h-0 overflow-hidden opacity-0 transition-[max-height,opacity] duration-300 group-hover:max-h-40 group-hover:opacity-100 group-focus-within:max-h-40 group-focus-within:opacity-100">
                  <p className="pt-2 text-xs leading-5 text-slate-500">{service.details}</p>
                </div>
                <button type="button" onClick={() => onBookConsultation(service.title)}
                  className="mt-4 inline-flex min-h-10 items-center gap-2 text-sm font-bold text-emerald-700 transition-colors hover:text-emerald-900">
                  <span>Book Appointment</span> <ArrowRight size={16} aria-hidden="true" />
                </button>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}

import { Clock3, ExternalLink, MapPin, Phone, Star } from 'lucide-react';
import { clinicDetails, clinicLocations, justDialProfileUrl } from '../../utils/clinic';

const clinicPhones = [
  import.meta.env.VITE_CLINIC_PHONE_PRIMARY || '+91-98107-64715',
  import.meta.env.VITE_CLINIC_PHONE_SECONDARY || '+91-97177-73179',
];

const items = [
  { icon: MapPin, title: 'Clinic 1 · Sec-44, Chhalera', detail: clinicDetails.SEC_44.address },
  { icon: Clock3, title: 'Clinic 2 · Sec-82, Madhuban', detail: clinicDetails.SEC_82.address },
];

export default function ClinicContactBar() {
  return (
    <section className="mx-auto max-w-7xl px-5 py-10 sm:px-8 sm:py-12" aria-label="Clinic contact information">
      <div className="grid gap-4 rounded-3xl border border-clinic-ink/5 bg-white p-5 shadow-sm sm:grid-cols-2 sm:p-7 lg:grid-cols-4">
        {items.map(({ icon: Icon, title, detail }, index) => {
          const location = clinicLocations[index]!;
          return (
          <div key={title} className="flex items-start gap-4">
            <span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-clinic-mint text-clinic-green">
              <Icon size={20} aria-hidden="true" />
            </span>
            <div>
              <h2 className="font-bold text-clinic-ink">{title}</h2>
              <p className="mt-1 text-sm leading-6 text-slate-600">{detail}</p>
              <p className="mt-1 text-xs leading-5 text-slate-500">{clinicDetails[location].hours}</p>
              <a href={clinicDetails[location].directions} target="_blank" rel="noreferrer" className="mt-1 inline-block text-xs font-semibold text-clinic-green">Map directions</a>
            </div>
          </div>
          );
        })}
        <div className="flex items-start gap-4">
          <span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-amber-50 text-amber-700">
            <Phone size={20} aria-hidden="true" />
          </span>
          <div>
            <h2 className="font-bold text-clinic-ink">Clinic helpline</h2>
            {clinicPhones.map((phone) => (
              <a key={phone} href={`tel:${phone.replace(/[^\d+]/g, '')}`} className="mt-1 block text-sm font-semibold text-clinic-green hover:text-clinic-ink">
                {phone}
              </a>
            ))}
            <p className="mt-1 text-xs leading-5 text-slate-500">Call the clinic for appointments and urgent dental concerns.</p>
          </div>
        </div>
        <a href={justDialProfileUrl} target="_blank" rel="noreferrer"
          className="flex items-start gap-4 rounded-2xl border border-amber-100 bg-amber-50/50 p-3 transition hover:border-amber-300 hover:bg-amber-50">
          <span className="grid h-11 w-11 shrink-0 place-items-center rounded-2xl bg-white text-amber-500">
            <Star size={20} fill="currentColor" aria-hidden="true" />
          </span>
          <span className="min-w-0">
            <strong className="flex items-center gap-1 text-sm text-clinic-ink">4.8 / 5.0 Rating
              <ExternalLink className="h-3.5 w-3.5 text-amber-500" aria-hidden="true" />
            </strong>
            <span className="mt-1 block text-xs leading-5 text-slate-600">50+ Verified Patient Reviews on JustDial</span>
            <span className="mt-1 block text-xs leading-5 text-slate-500">Near Chauhan Stationary, Sector 44, Noida</span>
          </span>
        </a>
      </div>
    </section>
  );
}

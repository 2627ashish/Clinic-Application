import { useEffect, useState } from 'react';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { DENTAL_IMAGES } from '../../constants/dentalImages';

const slides = [
  {
    badge: '🦷 Surgical Extraction Protocol',
    title: 'Safe Healing & Dry Socket Prevention',
    image: DENTAL_IMAGES.services.extraction,
    points: [
      '⏱️ 60 मिनट तक रुई दबा कर रखें (Keep gauze pressed for 60 mins)',
      '🚫 थूकना, कुल्ला करना और स्ट्रॉ का प्रयोग बिल्कुल न करें (No spitting or straw)',
      '🧊 गाल के बाहर 15-20 मिनट बर्फ की सिकाई करें (Ice pack externally)',
      '🥣 24 घंटे केवल ठंडा व नरम आहार लें (Soft, cold food only)',
    ],
  },
  {
    badge: '✨ Post-RCT Care Guide',
    title: 'Long-Term Tooth Protection & Crown Advice',
    image: DENTAL_IMAGES.services.rct,
    points: [
      '⏳ सुन्नपन (Anesthesia) खत्म होने के बाद ही भोजन करें',
      '🚫 जिस तरफ RCT हुआ है, उधर से कड़क चीजें (बर्फ, सुपारी) न चबाएं',
      '👑 समय पर Permanent Crown (कैप) अवश्य लगवाएं ताकि दांत न टूटे',
      '💊 डॉक्टर द्वारा दी गई दवाइयां समय पर लें',
    ],
  },
  {
    badge: '⚠️ Emergency Checklist',
    title: 'Signs Requiring Immediate Attention',
    image: DENTAL_IMAGES.clinicAmbience,
    points: [
      '🩸 लगातार खून बहना',
      '⚡ 3-4 दिन बाद अचानक तेज दर्द (Dry Socket)',
      '🌡️ तेज बुखार या मसूड़े में पस',
    ],
  },
];

export default function PostCareCarousel() {
  const [activeSlide, setActiveSlide] = useState(0);

  useEffect(() => {
    const timer = window.setInterval(() => {
      setActiveSlide((current) => (current + 1) % slides.length);
    }, 5000);
    return () => window.clearInterval(timer);
  }, []);

  function move(direction: -1 | 1) {
    setActiveSlide((current) => (current + direction + slides.length) % slides.length);
  }

  const slide = slides[activeSlide]!;

  return (
    <section className="relative mx-auto max-w-5xl overflow-hidden rounded-3xl border border-slate-200/80 bg-white shadow-xl"
      aria-label="Dental treatment after-care" aria-roledescription="carousel">
      <header className="absolute inset-x-0 top-0 z-20 px-5 pt-5 sm:px-8">
        <p className="text-sm font-bold tracking-wide text-slate-700">A comfortable visit starts here</p>
      </header>
      <article key={slide.title} className="relative min-h-[420px] sm:min-h-[390px]" aria-live="polite">
        <img src={slide.image} alt="" className="absolute inset-0 h-full w-full object-cover object-right" aria-hidden="true" />
        <div className="absolute inset-0 bg-gradient-to-r from-white via-white/95 to-white/40" aria-hidden="true" />
        <div className="relative z-10 flex min-h-[420px] flex-col justify-center px-5 pb-14 pt-16 sm:min-h-[390px] sm:px-10 sm:py-16">
          <div className="max-w-3xl">
            <span className="inline-flex rounded-full border border-emerald-100 bg-white/80 px-3 py-1.5 text-xs font-bold text-emerald-800 shadow-sm backdrop-blur">
              {slide.badge}
            </span>
            <h2 className="mt-3 max-w-2xl text-2xl font-extrabold leading-tight tracking-tight text-slate-900 sm:text-3xl">
              {slide.title}
            </h2>
            <ul className="mt-4 grid gap-2 text-sm leading-6 text-slate-700 sm:max-w-2xl sm:grid-cols-2">
              {slide.points.map((point) => (
                <li key={point} className="rounded-xl border border-white/70 bg-white/75 px-3 py-2 shadow-sm backdrop-blur-sm">
                  {point}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </article>
      <footer className="absolute inset-x-0 bottom-0 z-20 flex justify-center pb-4">
        <div className="flex items-center gap-2 rounded-full border border-white/80 bg-white/85 px-2 py-1.5 shadow-lg backdrop-blur-md" role="group" aria-label="After-care carousel controls">
          <button type="button" onClick={() => move(-1)} aria-label="Previous after-care slide"
            className="grid h-8 w-8 place-items-center rounded-full text-slate-600 transition hover:bg-emerald-50 hover:text-emerald-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-emerald-600">
            <ChevronLeft size={18} />
          </button>
          <div className="flex items-center gap-2 px-1" role="group" aria-label="Choose after-care slide">
            {slides.map((item, index) => (
              <button key={item.title} type="button" aria-label={`Show slide ${index + 1}`} aria-current={activeSlide === index}
                onClick={() => setActiveSlide(index)}
                className={`h-2.5 rounded-full transition-all duration-300 ${activeSlide === index ? 'w-8 bg-emerald-700' : 'w-2.5 bg-slate-300 hover:bg-slate-400'}`} />
            ))}
          </div>
          <button type="button" onClick={() => move(1)} aria-label="Next after-care slide"
            className="grid h-8 w-8 place-items-center rounded-full text-slate-600 transition hover:bg-emerald-50 hover:text-emerald-800 focus-visible:outline focus-visible:outline-2 focus-visible:outline-emerald-600">
            <ChevronRight size={18} />
          </button>
        </div>
      </footer>
    </section>
  );
}

import { useState } from 'react';
import { CheckCircle2, ChevronDown, XCircle } from 'lucide-react';

const procedures = [
  {
    title: 'Root Canal Treatment (RCT) / रूट कैनाल के बाद देखभाल',
    dos: ['Take medicines only as prescribed / दवाएँ केवल डॉक्टर के निर्देशानुसार लें', 'Chew on the other side until the permanent filling or crown is placed / स्थायी फिलिंग या कैप लगने तक दूसरी तरफ से चबाएँ'],
    donts: ['Do not chew hard or sticky foods on the treated tooth / उपचारित दाँत से कठोर या चिपचिपी चीज़ें न चबाएँ', 'Do not delay the advised follow-up / बताई गई अगली जाँच में देरी न करें'],
  },
  {
    title: 'Tooth Extraction / दाँत निकालने के बाद सावधानियां',
    dos: ['Bite gently on the gauze as directed and rest after the procedure / निर्देशानुसार गॉज़ दबाएँ और आराम करें', 'Use a cold pack outside the cheek if advised / सलाह मिलने पर गाल के बाहर ठंडी सिकाई करें'],
    donts: ['Avoid smoking, spitting, straws, and vigorous rinsing on the first day / पहले दिन धूम्रपान, थूकना, स्ट्रॉ और ज़ोर से कुल्ला करने से बचें', 'Do not touch the socket with your fingers or tongue / घाव को उँगली या जीभ से न छुएँ'],
  },
  {
    title: 'Complete & Partial Dentures / बत्तीसी की देखभाल',
    dos: ['Clean dentures daily and clean your gums and tongue / बत्तीसी और मसूड़ों व जीभ की रोज़ सफाई करें', 'Remove dentures at night unless your dentist advises otherwise / डॉक्टर की अलग सलाह न हो तो रात में बत्तीसी निकालें'],
    donts: ['Do not use hot water or household bleach to clean dentures / गर्म पानी या घरेलू ब्लीच से बत्तीसी साफ न करें', 'Do not adjust a loose or painful denture yourself / ढीली या दर्द करने वाली बत्तीसी को खुद न तराशें'],
  },
  {
    title: 'Teeth Scaling & Polishing / दाँतों की सफाई के बाद',
    dos: ['Brush gently with a soft-bristled brush and keep up daily oral hygiene / मुलायम ब्रश से धीरे साफ करें और रोज़ मुँह की सफाई रखें', 'Contact the clinic if sensitivity persists or worsens / संवेदनशीलता बनी रहे या बढ़े तो क्लिनिक से संपर्क करें'],
    donts: ['Avoid very hot, cold, or strongly coloured foods if teeth feel sensitive / संवेदनशीलता हो तो बहुत गर्म, ठंडे या गहरे रंग के खाद्य पदार्थों से बचें', 'Do not skip brushing because of mild temporary sensitivity / हल्की अस्थायी संवेदनशीलता के कारण ब्रश करना न छोड़ें'],
  },
  {
    title: 'Dental Restorations / फिलिंग के बाद नियम',
    dos: ['Follow the dentist’s instructions about eating while numb / सुन्नपन रहने तक खाने के बारे में डॉक्टर के निर्देश मानें', 'Call the clinic if your bite feels uneven / काटने पर ऊँच-नीच लगे तो क्लिनिक को फोन करें'],
    donts: ['Do not bite hard foods until the numbness wears off / सुन्नपन उतरने तक कठोर चीज़ें न काटें', 'Do not ignore persistent pain or a loose restoration / लगातार दर्द या ढीली फिलिंग को नज़रअंदाज़ न करें'],
  },
  {
    title: 'Crown & Bridge Capping / कैपिंग के बाद सावधानियां',
    dos: ['Keep the area clean and attend the crown-fitting follow-up / जगह साफ रखें और कैप फिटिंग की अगली जाँच पर आएँ', 'Chew carefully until the final crown is fitted / स्थायी कैप लगने तक सावधानी से चबाएँ'],
    donts: ['Avoid sticky or very hard foods on a temporary crown / अस्थायी कैप पर चिपचिपी या बहुत कठोर चीज़ें न खाएँ', 'Do not use household glue if a crown comes off / कैप निकलने पर घरेलू गोंद न लगाएँ'],
  },
];

export default function DentalFaqSection() {
  const [openTitle, setOpenTitle] = useState(procedures[0]?.title ?? '');

  return (
    <section className="mx-auto max-w-7xl px-5 py-12 sm:px-8 sm:py-16" aria-labelledby="dental-faq-title">
      <div className="mx-auto max-w-3xl text-center">
        <p className="inline-flex items-center gap-2 rounded-full bg-clinic-mint px-4 py-2 text-xs font-bold uppercase tracking-[0.16em] text-clinic-green">🦷 Aftercare guide · देखभाल</p>
        <h2 id="dental-faq-title" className="mt-4 text-3xl font-bold tracking-tight text-clinic-ink sm:text-4xl">Care after your dental treatment</h2>
        <p className="mt-2 text-sm leading-6 text-slate-600">General guidance only. Follow your dentist’s specific instructions and contact the clinic if symptoms worsen.</p>
      </div>
      <div className="mx-auto mt-8 max-w-4xl divide-y divide-slate-200 border-y border-slate-200">
        {procedures.map((item, index) => {
          const isOpen = openTitle === item.title;
          const rowId = `aftercare-${index}`;
          return (
            <article key={item.title} className="bg-white transition-colors hover:bg-clinic-paper/60">
              <button type="button" aria-expanded={isOpen} aria-controls={rowId} onClick={() => setOpenTitle(isOpen ? '' : item.title)}
                className="group flex min-h-20 w-full items-center justify-between gap-4 px-4 py-4 text-left sm:px-6">
                <span className="flex min-w-0 items-center gap-4">
                  <span aria-hidden="true" className="grid h-10 w-10 shrink-0 place-items-center rounded-full bg-clinic-mint text-xl">🦷</span>
                  <span className="font-semibold leading-6 text-clinic-ink group-hover:text-clinic-green">{item.title}</span>
                </span>
                <ChevronDown size={19} className={`shrink-0 text-clinic-green transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`} />
              </button>
              <div id={rowId} hidden={!isOpen} className="border-t border-slate-100 bg-clinic-paper/50 px-4 py-5 sm:px-6 sm:py-6">
                  <div className="space-y-5">
                    <div>
                      <h3 className="flex items-center gap-2 font-bold text-emerald-800"><CheckCircle2 size={18} />Dos / क्या करें</h3>
                      <ul className="mt-3 space-y-3 pl-7 text-sm leading-6 text-slate-700">{item.dos.map((tip) => <li key={tip} className="list-disc marker:text-emerald-600">{tip}</li>)}</ul>
                    </div>
                    <div className="border-t border-slate-200 pt-5">
                      <h3 className="flex items-center gap-2 font-bold text-rose-800"><XCircle size={18} />Don’ts / क्या न करें</h3>
                      <ul className="mt-3 space-y-3 pl-7 text-sm leading-6 text-slate-700">{item.donts.map((tip) => <li key={tip} className="list-disc marker:text-rose-600">{tip}</li>)}</ul>
                    </div>
                  </div>
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}

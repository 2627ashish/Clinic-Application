export type GuidanceLanguage = 'en' | 'hi';

interface Guidance {
  keywords: string[];
  title: Record<GuidanceLanguage, string>;
  advice: Record<GuidanceLanguage, string>;
}

const guidance: Guidance[] = [
  {
    keywords: ['toothache', 'tooth pain', 'severe pain', 'दाँत दर्द', 'दांत दर्द', 'दर्द'],
    title: { en: 'Toothache', hi: 'दाँत दर्द' },
    advice: {
      en: 'Gently rinse with warm salt water and use a cold pack on the cheek. Clove oil may soothe the area for some people; do not put aspirin directly on your gums. Arrange a dental evaluation, especially for severe or worsening pain.',
      hi: 'हल्के गुनगुने नमक के पानी से कुल्ला करें और गाल पर ठंडी सिकाई करें। कुछ लोगों को लौंग के तेल से आराम मिल सकता है; एस्पिरिन सीधे मसूड़े पर न रखें। तेज़ या बढ़ते दर्द में दंत चिकित्सक से जाँच कराएँ।',
    },
  },
  {
    keywords: ['bleeding gum', 'bleeding gums', 'gum bleed', 'मसूड़ों से खून', 'मसूड़े से खून'],
    title: { en: 'Bleeding gums', hi: 'मसूड़ों से खून आना' },
    advice: {
      en: 'Use an ultra-soft brush and rinse gently with salt water. Persistent bleeding can have several causes, including gum inflammation, so please arrange a dental check-up.',
      hi: 'अल्ट्रा-सॉफ्ट ब्रश का उपयोग करें और नमक के पानी से धीरे कुल्ला करें। लगातार खून आने के कई कारण हो सकते हैं; दंत चिकित्सक से जाँच कराएँ।',
    },
  },
  {
    keywords: ['sensitivity', 'sensitive teeth', 'झनझनाहट', 'दांत में झनझन'],
    title: { en: 'Teeth sensitivity', hi: 'दाँतों में झनझनाहट' },
    advice: {
      en: 'Avoid very hot or cold foods for now. Consider desensitizing toothpaste and a soft-bristled brush; book a check-up if symptoms persist.',
      hi: 'अभी बहुत ठंडा या गर्म खाने से बचें। सेंसिटिविटी टूथपेस्ट और मुलायम ब्रश का उपयोग करें; परेशानी बनी रहे तो जाँच कराएँ।',
    },
  },
  {
    keywords: ['swelling', 'swollen', 'face swollen', 'gum swollen', 'सूजन', 'चेहरे में सूजन', 'मसूड़े में सूजन'],
    title: { en: 'Facial or gum swelling', hi: 'चेहरे या मसूड़े में सूजन' },
    advice: {
      en: 'Swelling can signal a problem that needs prompt dental assessment. Keep your head elevated, do not apply heat, and contact a dentist urgently. Trouble breathing or swallowing requires emergency care now.',
      hi: 'सूजन ऐसी समस्या का संकेत हो सकती है जिसके लिए तुरंत दंत जाँच ज़रूरी है। सिर ऊँचा रखें, गर्म सिकाई न करें और तुरंत दंत चिकित्सक से संपर्क करें। साँस लेने या निगलने में परेशानी हो तो अभी आपातकालीन सहायता लें।',
    },
  },
];

export function getFirstAidGuidance(symptoms: string, language: GuidanceLanguage): {
  title: string;
  advice: string;
} {
  const normalized = symptoms.toLocaleLowerCase();
  const match = guidance.find((item) => item.keywords.some((keyword) =>
    normalized.includes(keyword.toLocaleLowerCase()),
  ));
  if (match) return { title: match.title[language], advice: match.advice[language] };

  return language === 'en'
    ? {
      title: 'Dental concern',
      advice: 'Avoid trying to diagnose or treat the cause yourself. Keep the area clean and arrange a dental assessment. Seek urgent care for facial swelling, fever, or trouble breathing or swallowing.',
    }
    : {
      title: 'दाँत से जुड़ी परेशानी',
      advice: 'कारण का स्वयं निदान या उपचार करने से बचें। जगह साफ रखें और दंत जाँच कराएँ। चेहरे पर सूजन, बुखार, या साँस लेने/निगलने में परेशानी हो तो तुरंत सहायता लें।',
    };
}

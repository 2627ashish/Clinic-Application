import { useEffect, useRef, useState } from 'react';
import { Mic, MicOff } from 'lucide-react';

interface SpeechAlternativeLike {
  transcript: string;
}

interface SpeechResultLike extends ArrayLike<SpeechAlternativeLike> {
  isFinal: boolean;
}

interface SpeechResultEventLike {
  results: ArrayLike<SpeechResultLike>;
}

interface SpeechErrorEventLike {
  error: string;
}

interface SpeechRecognitionLike {
  continuous: boolean;
  interimResults: boolean;
  lang: string;
  onstart: (() => void) | null;
  onend: (() => void) | null;
  onerror: ((event: SpeechErrorEventLike) => void) | null;
  onresult: ((event: SpeechResultEventLike) => void) | null;
  start: () => void;
  stop: () => void;
}

interface SpeechWindow extends Window {
  SpeechRecognition?: new () => SpeechRecognitionLike;
  webkitSpeechRecognition?: new () => SpeechRecognitionLike;
}

interface VoiceMicButtonProps {
  value: string;
  onChange: (value: string) => void;
  language?: 'en' | 'hi';
}

const symptomTags = ['Severe Toothache', 'Bleeding Gums', 'Broken Tooth', 'Routine Cleaning'];
const hindiSymptomTags = ['दाँत दर्द', 'मसूड़ों से खून', 'दाँत टूटना', 'सफाई'];

function speechErrorMessage(code: string): string {
  if (code === 'not-allowed' || code === 'service-not-allowed') {
    return 'Microphone access was denied. Allow microphone access in your browser, or type your concern below.';
  }
  if (code === 'no-speech') return 'We did not hear speech. Try again or type your concern below.';
  if (code === 'audio-capture') return 'No microphone was found. Type your concern below instead.';
  return 'Voice input is unavailable right now. You can type your concern below.';
}

export default function VoiceMicButton({ value, onChange, language = 'en' }: VoiceMicButtonProps) {
  const [supported, setSupported] = useState(false);
  const [recording, setRecording] = useState(false);
  const [error, setError] = useState('');
  const recognitionRef = useRef<SpeechRecognitionLike | null>(null);

  useEffect(() => {
    const speechWindow = window as SpeechWindow;
    const Recognition = speechWindow.SpeechRecognition ?? speechWindow.webkitSpeechRecognition;
    if (!Recognition) return;

    const recognition = new Recognition();
    let started = false;
    recognition.lang = language === 'hi' ? 'hi-IN' : 'en-IN';
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.onstart = () => {
      started = true;
      setRecording(true);
      setError('');
    };
    recognition.onend = () => {
      started = false;
      setRecording(false);
    };
    recognition.onerror = (event) => {
      started = false;
      setRecording(false);
      setError(speechErrorMessage(event.error));
    };
    recognition.onresult = (event) => {
      const transcript = Array.from(event.results)
        .map((result) => result[0]?.transcript ?? '')
        .filter(Boolean)
        .join(' ')
        .trim();
      if (transcript) onChange(transcript);
    };
    recognitionRef.current = recognition;
    setSupported(true);

    return () => {
      recognition.onresult = null;
      recognition.onend = null;
      recognition.onerror = null;
      if (started) recognition.stop();
      recognitionRef.current = null;
    };
  }, [language, onChange]);

  function toggleRecording() {
    const recognition = recognitionRef.current;
    if (!recognition) return;
    setError('');
    if (recording) {
      recognition.stop();
      return;
    }
    try {
      recognition.start();
      setRecording(true);
    } catch {
      setRecording(false);
      setError('Voice input could not start. Type your concern below instead.');
    }
  }

  return (
    <div className="space-y-3">
      {supported && (
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={toggleRecording}
            aria-pressed={recording}
            aria-label={recording ? 'Stop voice input' : 'Start voice input'}
            className={`relative grid h-12 w-12 shrink-0 place-items-center rounded-full text-white transition ${
              recording ? 'bg-red-600 shadow-[0_0_0_7px_rgba(220,38,38,0.12)]' : 'bg-clinic-green hover:bg-clinic-ink'
            }`}
          >
            {recording && <span className="absolute inset-0 animate-ping rounded-full bg-red-500/30" aria-hidden="true" />}
            {recording ? <MicOff size={20} aria-hidden="true" /> : <Mic size={20} aria-hidden="true" />}
          </button>
          <div aria-live="polite">
            <p className="text-sm font-semibold text-clinic-ink">{recording ? 'Listening… speak naturally' : 'Speak your concern'}</p>
            <p className="text-xs text-slate-500">Live transcription appears in the text box below.</p>
          </div>
        </div>
      )}

      <label htmlFor="symptoms" className="block text-sm font-semibold text-clinic-ink">
        {recording ? 'Live transcription' : 'Your dental concern'}
        <textarea
          id="symptoms"
          required
          rows={4}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          className="mt-1.5 w-full rounded-xl border border-slate-200 bg-white px-3.5 py-3 text-sm text-clinic-ink placeholder:text-slate-400 focus:border-clinic-green focus:outline-none focus:ring-2 focus:ring-clinic-green/15"
          placeholder={language === 'hi' ? 'दर्द कहाँ है और कब से हो रहा है, बताएं…' : 'Describe the pain, where it is, and when it started…'}
        />
      </label>

      {(!supported || error) && (
        <div>
          <p className="mb-2 text-xs font-medium text-slate-500">
            {supported ? error : language === 'hi' ? 'इस ब्राउज़र में आवाज़ उपलब्ध नहीं है। लिखें या नीचे विकल्प चुनें।' : 'Voice input is not supported in this browser. Type a concern or choose one below.'}
          </p>
          <div className="flex flex-wrap gap-2">
            {(language === 'hi' ? hindiSymptomTags : symptomTags).map((tag) => (
              <button
                key={tag}
                type="button"
                onClick={() => onChange(tag)}
                className="rounded-full border border-slate-200 px-3 py-1.5 text-xs font-semibold text-slate-600 transition hover:border-clinic-green hover:bg-clinic-mint hover:text-clinic-green"
              >
                {tag}
              </button>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

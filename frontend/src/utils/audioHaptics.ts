let audioContext: AudioContext | null = null;

function getAudioContext(): AudioContext | null {
  if (audioContext) return audioContext;
  const AudioContextConstructor = window.AudioContext;
  if (!AudioContextConstructor) return null;
  audioContext = new AudioContextConstructor();
  return audioContext;
}

export function playWheelTickSound(): void {
  if (typeof navigator !== 'undefined') navigator.vibrate?.(5);
  if (typeof window === 'undefined') return;

  try {
    const context = getAudioContext();
    if (!context) return;
    const play = () => {
      const start = context.currentTime;
      const oscillator = context.createOscillator();
      const gain = context.createGain();
      oscillator.type = 'triangle';
      oscillator.frequency.setValueAtTime(1200, start);
      gain.gain.setValueAtTime(0.045, start);
      gain.gain.exponentialRampToValueAtTime(0.0001, start + 0.007);
      oscillator.connect(gain);
      gain.connect(context.destination);
      oscillator.start(start);
      oscillator.stop(start + 0.008);
    };
    if (context.state === 'suspended') {
      void context.resume().then(play).catch(() => undefined);
    } else {
      play();
    }
  } catch {
    // Audio and vibration are optional browser capabilities.
  }
}

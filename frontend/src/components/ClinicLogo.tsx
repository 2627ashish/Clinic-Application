import { useState } from 'react';

interface ClinicLogoProps {
  className?: string;
}

export default function ClinicLogo({ className = '' }: ClinicLogoProps) {
  const [unavailable, setUnavailable] = useState(false);

  if (unavailable) {
    return (
      <span className={`inline-flex items-center gap-2 ${className}`} role="img" aria-label="Chauhan Dental Clinic">
        <svg viewBox="0 0 48 48" className="h-full max-h-12 w-auto shrink-0" fill="none" aria-hidden="true">
          <path d="M12 8c-6 3-5 11-3 17l4 14c.7 2 3.3 2 4 0l3-10c.7-2 3.3-2 4 0l3 10c.7 2 3.3 2 4 0l4-14c2-6 3-14-3-17-4-2-7 1-10 1S16 6 12 8Z" stroke="#2563eb" strokeWidth="3" strokeLinejoin="round" />
          <path d="M16 23c3 4 13 4 16 0" stroke="#059669" strokeWidth="2.5" strokeLinecap="round" />
        </svg>
        <span className="font-extrabold leading-tight text-emerald-700">
          CHAUHAN
          <span className="block text-[0.62em] tracking-[0.18em]">DENTAL CLINIC</span>
        </span>
      </span>
    );
  }

  return (
    <img src="/images/logo.png" alt="Chauhan Dental Clinic" className={`object-contain ${className}`}
      onError={() => setUnavailable(true)} />
  );
}

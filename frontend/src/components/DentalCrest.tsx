interface DentalCrestProps {
  className?: string;
  iconClassName?: string;
}

export default function DentalCrest({
  className = '',
  iconClassName = 'h-6 w-6',
}: DentalCrestProps) {
  return (
    <span className={`grid place-items-center rounded-2xl bg-clinic-green text-white ${className}`} aria-hidden="true">
      <svg viewBox="0 0 48 48" className={iconClassName} fill="none">
        <path
          d="M12.1 8.7c-5.8 3.1-5.5 10.1-3.6 16.4l4.3 14.1c.6 2 3.4 2.2 4.1.1l3.2-9.6c.6-1.8 3.2-1.8 3.8 0l3.2 9.6c.7 2.1 3.5 1.9 4.1-.1l4.3-14.1c1.9-6.3 2.2-13.3-3.6-16.4-4.1-2.2-7.5.8-9.9.8s-5.8-3-9.9-.8Z"
          stroke="currentColor"
          strokeWidth="2.6"
          strokeLinejoin="round"
        />
        <path d="M16.5 23.2c2.4 4.1 12.6 4.1 15 0" stroke="currentColor" strokeWidth="2.4" strokeLinecap="round" />
        <path d="M20 20h.01M28 20h.01" stroke="currentColor" strokeWidth="3.2" strokeLinecap="round" />
      </svg>
    </span>
  );
}

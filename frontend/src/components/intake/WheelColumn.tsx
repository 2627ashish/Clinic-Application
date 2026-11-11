import { useEffect, useRef } from 'react';
import type { KeyboardEvent } from 'react';
import { playWheelTickSound } from '../../utils/audioHaptics';

export interface WheelOption {
  value: string;
  label: string;
  disabled?: boolean;
}

interface WheelColumnProps {
  label: string;
  options: WheelOption[];
  selectedIndex: number;
  onSelect: (index: number) => void;
}

const ROW_HEIGHT = 48;

export default function WheelColumn({ label, options, selectedIndex, onSelect }: WheelColumnProps) {
  const wheelRef = useRef<HTMLDivElement>(null);
  const lastTickIndex = useRef(selectedIndex);

  useEffect(() => {
    const wheel = wheelRef.current;
    if (wheel) wheel.scrollTop = selectedIndex * ROW_HEIGHT;
    lastTickIndex.current = selectedIndex;
  }, [selectedIndex]);

  function select(index: number, smooth = false) {
    const option = options[index];
    if (!option || option.disabled) return;
    if (index !== lastTickIndex.current) {
      playWheelTickSound();
      lastTickIndex.current = index;
    }
    onSelect(index);
    wheelRef.current?.scrollTo({
      top: index * ROW_HEIGHT,
      behavior: smooth ? 'smooth' : 'auto',
    });
  }

  function handleScroll() {
    const wheel = wheelRef.current;
    if (!wheel) return;
    const index = Math.max(0, Math.min(options.length - 1, Math.round(wheel.scrollTop / ROW_HEIGHT)));
    if (options[index]?.disabled) {
      wheel.scrollTo({ top: selectedIndex * ROW_HEIGHT });
      return;
    }
    if (index === lastTickIndex.current) return;
    const ticks = Math.min(Math.abs(index - lastTickIndex.current), options.length);
    for (let tick = 0; tick < ticks; tick += 1) playWheelTickSound();
    lastTickIndex.current = index;
    onSelect(index);
  }

  function handleKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key !== 'ArrowUp' && event.key !== 'ArrowDown') return;
    event.preventDefault();
    const direction = event.key === 'ArrowDown' ? 1 : -1;
    let index = selectedIndex + direction;
    while (index >= 0 && index < options.length && options[index]?.disabled) index += direction;
    if (index >= 0 && index < options.length) select(index, true);
  }

  return (
    <div className="min-w-0">
      <p className="mb-2 text-center text-xs font-bold uppercase tracking-[0.14em] text-slate-500">{label}</p>
      <div
        ref={wheelRef}
        role="listbox"
        aria-label={label}
        aria-activedescendant={`${label.replace(/\s+/g, '-')}-${options[selectedIndex]?.value ?? 'selected'}`}
        tabIndex={0}
        onScroll={handleScroll}
        onKeyDown={handleKeyDown}
        className="wheel-column relative h-[240px] overflow-y-auto rounded-xl outline-none snap-y snap-mandatory focus-visible:ring-2 focus-visible:ring-clinic-green/40"
      >
        <div aria-hidden="true" className="wheel-selection-highlight pointer-events-none absolute inset-x-1 top-1/2 z-10 h-12 -translate-y-1/2 rounded-lg border-y border-clinic-green/25" />
        <div aria-hidden="true" className="h-24" />
        {options.map((option, index) => (
          <button
            id={`${label.replace(/\s+/g, '-')}-${option.value}`}
            key={option.value}
            type="button"
            role="option"
            aria-selected={selectedIndex === index}
            disabled={option.disabled}
            onClick={() => select(index)}
            className={`block h-12 w-full snap-center truncate px-2 text-center transition-colors ${
              selectedIndex === index ? 'text-base font-bold text-clinic-green' : 'text-sm text-slate-400'
            } disabled:cursor-not-allowed disabled:opacity-30`}
          >
            {option.label}
          </button>
        ))}
        <div aria-hidden="true" className="h-24" />
      </div>
    </div>
  );
}

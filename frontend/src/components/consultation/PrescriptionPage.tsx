import type { PointerEventHandler, Ref } from 'react';
import { facilities } from './prescriptionPadLayout';

interface PrescriptionPageProps {
  pageRef: Ref<HTMLElement>;
  canvasRef: Ref<HTMLCanvasElement>;
  patientName: string;
  patientAge: number | null;
  prescriptionDate: Date;
  onPointerDown: PointerEventHandler<HTMLCanvasElement>;
  onPointerMove: PointerEventHandler<HTMLCanvasElement>;
  onPointerUp: PointerEventHandler<HTMLCanvasElement>;
}

export default function PrescriptionPage({
  pageRef,
  canvasRef,
  patientName,
  patientAge,
  prescriptionDate,
  onPointerDown,
  onPointerMove,
  onPointerUp,
}: PrescriptionPageProps) {
  return (
    <article ref={pageRef} className="relative mx-auto my-3 min-h-[1152px] w-full max-w-[820px] overflow-hidden bg-white text-slate-800 shadow-2xl">
      <header className="flex h-[170px] flex-col justify-between bg-clinic-green px-4 py-3 text-white sm:px-7">
        <div className="text-center">
          <p className="text-2xl font-black tracking-wide sm:text-4xl">CHAUHAN DENTAL CLINIC</p>
          <p className="mt-1 text-xs font-semibold tracking-widest text-emerald-100">YOUR FAMILY DENTAL CARE</p>
        </div>
        <div className="grid grid-cols-2 items-end gap-3 rounded-t-[45%] bg-lime-100 px-3 py-2 text-center text-clinic-ink sm:px-6">
          <div>
            <p className="text-sm font-extrabold text-rose-800 sm:text-lg">DR. B.S. CHAUHAN (डॉ. बी. एस. चौहान)</p>
            <p className="text-[10px] font-semibold sm:text-xs">B.D.S (Noida) · Expert Dental Surgeon</p>
            <p className="text-[10px] sm:text-xs">Reg: A-5651</p>
          </div>
          <div className="text-[9px] font-semibold sm:text-xs">
            <p>CLINIC 1 · SEC-82, NOIDA</p><p>9:00 AM–10:00 AM · 2:00 PM–5:00 PM</p>
            <p className="mt-1">CLINIC 2 · SEC-44, CHHALERA</p><p>10:15 AM–1:00 PM · 6:00 PM–9:00 PM</p>
          </div>
        </div>
      </header>
      <section className="grid h-[62px] grid-cols-[minmax(0,1fr)_auto] items-center gap-x-4 gap-y-1 bg-slate-100 px-4 text-xs font-semibold sm:px-6 sm:text-sm">
        <p className="truncate">Name: {patientName}</p>
        <p>Age: {patientAge == null ? 'Not recorded' : `${patientAge} years`}</p>
        <p className="col-span-2">Date: {new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' }).format(prescriptionDate)}</p>
      </section>
      <div className="flex min-h-[820px]">
        <aside className="w-[105px] shrink-0 bg-slate-50 px-2 py-4 text-[9px] sm:w-[172px] sm:px-4 sm:text-[10px]">
          <h2 className="mb-3 text-xs font-extrabold uppercase text-rose-800">Facilities</h2>
          <ul className="space-y-2">{facilities.map((facility) => <li key={facility} className="leading-snug">• {facility}</li>)}</ul>
        </aside>
        <section className="relative min-w-0 flex-1">
          <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center text-clinic-green/10">
            <svg viewBox="0 0 100 100" className="h-40 w-40" fill="none" aria-hidden="true">
              <path d="M20 18c-12 8-7 27-2 42 5 17 10 29 18 26 7-2 5-23 14-23s7 21 14 23c8 3 13-9 18-26 5-15 10-34-2-42-10-7-20 1-30 1s-20-8-30-1Z" stroke="currentColor" strokeWidth="4" />
              <path d="M34 57c5 8 27 8 32 0" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
            </svg>
            <span className="mt-2 text-sm font-bold tracking-widest">CHAUHAN DENTAL CLINIC</span>
          </div>
          <p className="pointer-events-none absolute left-4 top-4 text-xl font-bold text-clinic-ink">Rx</p>
          <canvas
            ref={canvasRef}
            aria-label="Write your prescription with Apple Pencil or stylus."
            className="absolute inset-x-0 top-[56px] w-full"
            style={{ height: 'calc(100% - 56px)', touchAction: 'none' }}
            onPointerDown={onPointerDown}
            onPointerMove={onPointerMove}
            onPointerUp={onPointerUp}
            onPointerCancel={onPointerUp}
          />
        </section>
      </div>
      <footer className="flex min-h-[100px] flex-col justify-center bg-clinic-ink px-4 py-3 text-white sm:px-7">
        <p className="text-center text-sm font-extrabold text-lime-200">“Your smile, our commitment”</p>
        <p className="mt-2 text-[10px] font-semibold sm:text-xs">CLINIC 1 · 78A MIG, Madhuban Apartments, Sec-82, Noida</p>
        <p className="text-[10px] font-semibold sm:text-xs">CLINIC 2 · Gali No. 3, Chhalera, Sec-44, Noida</p>
      </footer>
    </article>
  );
}

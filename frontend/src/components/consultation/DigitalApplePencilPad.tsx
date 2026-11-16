import { useEffect, useRef, useState } from 'react';
import { Eraser, Highlighter, PenLine, RotateCcw, Save, Trash2, X } from 'lucide-react';
import { uploadHandwrittenPrescription } from '../../api/client';

type Tool = 'black' | 'blue' | 'highlighter' | 'eraser';
interface Point { x: number; y: number; pressure: number }
interface Stroke { tool: Tool; points: Point[] }

const toolStyles: Record<Tool, { color: string; width: number }> = {
  black: { color: '#111827', width: 2.2 },
  blue: { color: '#1d4ed8', width: 2.2 },
  highlighter: { color: '#fde047', width: 15 },
  eraser: { color: 'rgba(0,0,0,1)', width: 18 },
};

interface DigitalApplePencilPadProps {
  appointmentId: string;
  patientName: string;
  onClose: () => void;
  onSaved: () => void;
}

export default function DigitalApplePencilPad({ appointmentId, patientName, onClose, onSaved }: DigitalApplePencilPadProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const activeStrokeRef = useRef<Stroke | null>(null);
  const [tool, setTool] = useState<Tool>('black');
  const [strokes, setStrokes] = useState<Stroke[]>([]);
  const [activeStroke, setActiveStroke] = useState<Stroke | null>(null);
  const [canvasLayoutVersion, setCanvasLayoutVersion] = useState(0);
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const observer = new ResizeObserver(() => setCanvasLayoutVersion((version) => version + 1));
    observer.observe(canvas);
    return () => observer.disconnect();
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const context = canvas.getContext('2d');
    if (!context) return;
    const ratio = window.devicePixelRatio || 1;
    const bounds = canvas.getBoundingClientRect();
    canvas.width = Math.max(1, Math.round(bounds.width * ratio));
    canvas.height = Math.max(1, Math.round(bounds.height * ratio));
    context.setTransform(ratio, 0, 0, ratio, 0, 0);

    for (const stroke of [...strokes, ...(activeStroke ? [activeStroke] : [])]) {
      if (stroke.points.length === 0) continue;
      const style = toolStyles[stroke.tool];
      context.save();
      context.globalAlpha = stroke.tool === 'highlighter' ? 0.35 : 1;
      context.globalCompositeOperation = stroke.tool === 'eraser' ? 'destination-out' : 'source-over';
      context.strokeStyle = style.color;
      context.fillStyle = style.color;
      context.lineCap = 'round';
      context.lineJoin = 'round';
      const [first, ...rest] = stroke.points;
      if (first) {
        context.beginPath();
        context.arc(first.x, first.y, Math.max(style.width * (first.pressure || 0.5) / 2, 1), 0, Math.PI * 2);
        context.fill();
      }
      let previous = first;
      for (const point of rest) {
        if (!previous) continue;
        context.beginPath();
        context.lineWidth = Math.max(style.width * (point.pressure || 0.5), 1);
        context.moveTo(previous.x, previous.y);
        context.lineTo(point.x, point.y);
        context.stroke();
        previous = point;
      }
      context.restore();
    }
  }, [strokes, activeStroke, canvasLayoutVersion]);

  function pointerDown(event: React.PointerEvent<HTMLCanvasElement>) {
    event.currentTarget.setPointerCapture(event.pointerId);
    const bounds = event.currentTarget.getBoundingClientRect();
    const point = { x: event.clientX - bounds.left, y: event.clientY - bounds.top, pressure: event.pressure || 0.5 };
    const next = { tool, points: [point] };
    activeStrokeRef.current = next;
    setActiveStroke(next);
  }

  function pointerMove(event: React.PointerEvent<HTMLCanvasElement>) {
    if (!activeStrokeRef.current) return;
    const bounds = event.currentTarget.getBoundingClientRect();
    const point = { x: event.clientX - bounds.left, y: event.clientY - bounds.top, pressure: event.pressure || 0.5 };
    const next = { ...activeStrokeRef.current, points: [...activeStrokeRef.current.points, point] };
    activeStrokeRef.current = next;
    setActiveStroke(next);
  }

  function pointerUp() {
    if (activeStrokeRef.current) setStrokes((current) => [...current, activeStrokeRef.current!]);
    activeStrokeRef.current = null;
    setActiveStroke(null);
  }

  async function savePrescription() {
    const canvas = canvasRef.current;
    if (!canvas || strokes.length === 0) {
      setError('Write on the prescription pad before saving.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      const exportCanvas = document.createElement('canvas');
      const padBounds = canvas.parentElement?.getBoundingClientRect();
      if (!padBounds) throw new Error('Prescription page layout is unavailable');
      const ratio = window.devicePixelRatio || 1;
      exportCanvas.width = Math.max(1, Math.round(padBounds.width * ratio));
      exportCanvas.height = Math.max(1, Math.round(padBounds.height * ratio));
      const context = exportCanvas.getContext('2d');
      if (!context) throw new Error('Canvas export is unavailable');
      context.fillStyle = '#fff';
      context.fillRect(0, 0, exportCanvas.width, exportCanvas.height);
      context.scale(ratio, ratio);
      const width = padBounds.width;
      context.fillStyle = '#155e52';
      context.font = 'bold 22px sans-serif';
      context.textAlign = 'center';
      context.fillText('CHAUHAN DENTAL CLINIC', width / 2, 32);
      context.fillStyle = '#334155';
      context.font = '12px sans-serif';
      context.fillText('Dr. B.S. Chauhan · B.D.S (Noida) · Reg: A-5651', width / 2, 52);
      context.textAlign = 'left';
      context.fillText(`Patient: ${patientName}`, 24, 78);
      context.fillText(`Date: ${new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' }).format(new Date())}`, 24, 98);
      context.strokeStyle = '#d1d5db';
      context.beginPath();
      context.moveTo(20, 110);
      context.lineTo(width - 20, 110);
      context.stroke();
      context.drawImage(canvas, 0, 0, canvas.width, canvas.height, 0, 120, width, canvas.getBoundingClientRect().height);
      const blob = await new Promise<Blob>((resolve, reject) => {
        exportCanvas.toBlob((result) => result ? resolve(result) : reject(new Error('Could not export the prescription canvas')), 'image/png');
      });
      await uploadHandwrittenPrescription(appointmentId, blob);
      onSaved();
      onClose();
    } catch {
      setError('The handwritten prescription could not be uploaded. Please try again.');
    } finally {
      setSaving(false);
    }
  }

  const tools: { id: Tool; label: string; icon: typeof PenLine }[] = [
    { id: 'black', label: 'Black pen', icon: PenLine },
    { id: 'blue', label: 'Blue pen', icon: PenLine },
    { id: 'highlighter', label: 'Highlighter', icon: Highlighter },
    { id: 'eraser', label: 'Eraser', icon: Eraser },
  ];

  return (
    <div className="fixed inset-0 z-[70] flex flex-col bg-slate-900/80 p-2 sm:p-5" role="dialog" aria-modal="true" aria-label="Handwritten prescription pad">
      <header className="mx-auto flex w-full max-w-6xl flex-wrap items-center justify-between gap-3 rounded-t-2xl bg-white px-4 py-3">
        <div className="flex flex-wrap gap-2">
          {tools.map(({ id, label, icon: Icon }) => (
            <button key={id} type="button" onClick={() => setTool(id)} aria-pressed={tool === id}
              className={`inline-flex min-h-10 items-center gap-2 rounded-lg px-3 text-sm font-semibold ${tool === id ? 'bg-clinic-green text-white' : 'bg-slate-100 text-slate-700'}`}>
              <Icon size={16} />{label}
            </button>
          ))}
          <button type="button" onClick={() => { setStrokes((current) => current.slice(0, -1)); setActiveStroke(null); }}
            className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-slate-100 px-3 text-sm font-semibold text-slate-700"><RotateCcw size={16} />Undo</button>
          <button type="button" onClick={() => { setStrokes([]); setActiveStroke(null); }}
            className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-rose-50 px-3 text-sm font-semibold text-rose-700"><Trash2 size={16} />Clear</button>
        </div>
        <div className="flex gap-2">
          <button type="button" onClick={onClose} className="inline-flex min-h-10 items-center gap-2 rounded-lg border border-slate-200 px-3 text-sm font-semibold text-slate-700"><X size={16} />Close</button>
          <button type="button" disabled={saving} onClick={() => void savePrescription()} className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-clinic-green px-4 text-sm font-semibold text-white disabled:opacity-60"><Save size={16} />{saving ? 'Saving…' : 'Save Handwritten Rx'}</button>
        </div>
      </header>
      <div className="relative mx-auto min-h-0 w-full max-w-6xl flex-1 overflow-hidden rounded-b-2xl bg-white">
        <div className="pointer-events-none absolute inset-0 px-6 pt-5 text-slate-500">
          <div className="border-b border-slate-300 pb-3 text-center">
            <p className="text-xl font-extrabold tracking-wider text-clinic-green">CHAUHAN DENTAL CLINIC</p>
            <p className="mt-1 text-xs">Dr. B.S. Chauhan · B.D.S (Noida) · Reg: A-5651</p>
            <p className="mt-1 text-[10px]">Sector 44 · Sector 82 · Noida</p>
          </div>
          <div className="mt-2 flex justify-between text-xs"><span>Patient: __________________</span><span>Date: {new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' }).format(new Date())}</span></div>
          <div className="mt-2 text-sm font-semibold">℞</div>
          <p className="absolute bottom-4 left-6 right-6 border-t border-slate-300 pt-2 text-center text-[10px]">Your smile, our commitment · Follow your dentist’s instructions</p>
        </div>
        <canvas
          ref={canvasRef}
          aria-label="Write your prescription with Apple Pencil, stylus, or touch"
          className="absolute inset-x-0 bottom-10 top-[120px] w-full touch-none"
          onPointerDown={pointerDown}
          onPointerMove={pointerMove}
          onPointerUp={pointerUp}
          onPointerCancel={pointerUp}
        />
      </div>
      {error && <p role="alert" className="mx-auto w-full max-w-6xl rounded-b-xl bg-rose-50 px-4 py-2 text-sm font-medium text-rose-700">{error}</p>}
    </div>
  );
}

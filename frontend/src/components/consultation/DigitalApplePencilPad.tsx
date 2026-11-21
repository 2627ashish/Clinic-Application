import { useEffect, useRef, useState } from 'react';
import { Eraser, Highlighter, PenLine, RotateCcw, Save, Trash2, X } from 'lucide-react';
import { uploadHandwrittenPrescription } from '../../api/client';
import type { PatientDocumentView } from '../../types';
import PrescriptionPage from './PrescriptionPage';
import { drawPrescriptionPadTemplate } from './prescriptionPadLayout';

type Tool = 'black' | 'blue' | 'highlighter' | 'eraser';
interface Point { x: number; y: number; pressure: number }
interface Stroke { tool: Tool; points: Point[] }

const toolStyles: Record<Tool, { color: string; width: number }> = {
  black: { color: '#111827', width: 2.2 },
  blue: { color: '#1d4ed8', width: 2.2 },
  highlighter: { color: '#fde047', width: 15 },
  eraser: { color: 'rgba(0,0,0,1)', width: 18 },
};

function configureStroke(context: CanvasRenderingContext2D, stroke: Stroke) {
  const style = toolStyles[stroke.tool];
  context.globalAlpha = stroke.tool === 'highlighter' ? 0.35 : 1;
  context.globalCompositeOperation = stroke.tool === 'eraser' ? 'destination-out' : 'source-over';
  context.strokeStyle = style.color;
  context.fillStyle = style.color;
  context.lineCap = 'round';
  context.lineJoin = 'round';
}

function drawStrokePoint(context: CanvasRenderingContext2D, stroke: Stroke, point: Point) {
  const style = toolStyles[stroke.tool];
  context.save();
  configureStroke(context, stroke);
  context.beginPath();
  context.arc(point.x, point.y, Math.max(style.width * (point.pressure || 0.5) / 2, 1), 0, Math.PI * 2);
  context.fill();
  context.restore();
}

function drawStrokeSegment(context: CanvasRenderingContext2D, stroke: Stroke, from: Point, to: Point) {
  const style = toolStyles[stroke.tool];
  context.save();
  configureStroke(context, stroke);
  context.beginPath();
  context.lineWidth = Math.max(style.width * (to.pressure || 0.5), 1);
  context.moveTo(from.x, from.y);
  context.lineTo(to.x, to.y);
  context.stroke();
  context.restore();
}

function drawStroke(context: CanvasRenderingContext2D, stroke: Stroke) {
  const [first, ...rest] = stroke.points;
  if (!first) return;
  drawStrokePoint(context, stroke, first);
  let previous = first;
  for (const point of rest) {
    drawStrokeSegment(context, stroke, previous, point);
    previous = point;
  }
}

interface DigitalApplePencilPadProps {
  appointmentId: string;
  patientName: string;
  patientAge: number | null;
  onClose: () => void;
  onSaved: (document: PatientDocumentView) => void;
}

export default function DigitalApplePencilPad({
  appointmentId,
  patientName,
  patientAge,
  onClose,
  onSaved,
}: DigitalApplePencilPadProps) {
  const pageRef = useRef<HTMLElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const activeStrokeRef = useRef<Stroke | null>(null);
  const activePointerIdRef = useRef<number | null>(null);
  const [prescriptionDate] = useState(() => new Date());
  const [tool, setTool] = useState<Tool>('black');
  const [strokes, setStrokes] = useState<Stroke[]>([]);
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
    context.clearRect(0, 0, bounds.width, bounds.height);
    for (const stroke of strokes) drawStroke(context, stroke);
    if (activeStrokeRef.current) drawStroke(context, activeStrokeRef.current);
  }, [strokes, canvasLayoutVersion]);

  function pointerDown(event: React.PointerEvent<HTMLCanvasElement>) {
    if (activeStrokeRef.current) return;
    if (event.button !== 0 && event.pointerType !== 'pen') return;
    const context = event.currentTarget.getContext('2d');
    if (!context) {
      setError('Handwriting is not supported in this browser.');
      return;
    }
    event.currentTarget.setPointerCapture(event.pointerId);
    activePointerIdRef.current = event.pointerId;
    setError('');
    const bounds = event.currentTarget.getBoundingClientRect();
    const point = { x: event.clientX - bounds.left, y: event.clientY - bounds.top, pressure: event.pressure || 0.5 };
    const next = { tool, points: [point] };
    activeStrokeRef.current = next;
    drawStrokePoint(context, next, point);
  }

  function pointerMove(event: React.PointerEvent<HTMLCanvasElement>) {
    const stroke = activeStrokeRef.current;
    if (!stroke || activePointerIdRef.current !== event.pointerId) return;
    const bounds = event.currentTarget.getBoundingClientRect();
    const point = { x: event.clientX - bounds.left, y: event.clientY - bounds.top, pressure: event.pressure || 0.5 };
    const previous = stroke.points[stroke.points.length - 1];
    if (!previous) return;
    const context = event.currentTarget.getContext('2d');
    if (context) drawStrokeSegment(context, stroke, previous, point);
    const next = { ...stroke, points: [...stroke.points, point] };
    activeStrokeRef.current = next;
  }

  function pointerUp(event: React.PointerEvent<HTMLCanvasElement>) {
    if (activePointerIdRef.current !== event.pointerId) return;
    const completedStroke = activeStrokeRef.current;
    if (completedStroke) setStrokes((current) => [...current, completedStroke]);
    activeStrokeRef.current = null;
    activePointerIdRef.current = null;
  }

  async function savePrescription() {
    const canvas = canvasRef.current;
    const pendingStroke = activeStrokeRef.current;
    if (!canvas || (strokes.length === 0 && !pendingStroke)) {
      setError('Write on the prescription pad before saving.');
      return;
    }
    setSaving(true);
    setError('');
    try {
      const exportCanvas = document.createElement('canvas');
      const page = pageRef.current;
      const pageBounds = page?.getBoundingClientRect();
      if (!page || !pageBounds) throw new Error('Prescription page layout is unavailable');
      const writingBounds = canvas.getBoundingClientRect();
      const ratio = window.devicePixelRatio || 1;
      exportCanvas.width = Math.max(1, Math.round(pageBounds.width * ratio));
      exportCanvas.height = Math.max(1, Math.round(pageBounds.height * ratio));
      const context = exportCanvas.getContext('2d');
      if (!context) throw new Error('Canvas export is unavailable');
      const width = pageBounds.width;
      const height = pageBounds.height;
      context.scale(ratio, ratio);
      context.fillStyle = '#fff';
      context.fillRect(0, 0, width, height);
      drawPrescriptionPadTemplate(context, width, height, patientName, patientAge, prescriptionDate);
      context.drawImage(
        canvas,
        0,
        0,
        canvas.width,
        canvas.height,
        writingBounds.left - pageBounds.left,
        writingBounds.top - pageBounds.top,
        writingBounds.width,
        writingBounds.height,
      );
      const blob = await new Promise<Blob>((resolve, reject) => {
        exportCanvas.toBlob((result) => result ? resolve(result) : reject(new Error('Could not export the prescription canvas')), 'image/png');
      });
      const savedDocument = await uploadHandwrittenPrescription(appointmentId, blob);
      onSaved(savedDocument);
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
      <header className="mx-auto flex w-full max-w-6xl shrink-0 flex-wrap items-center justify-between gap-3 rounded-t-2xl bg-white px-4 py-3">
        <div className="flex flex-wrap gap-2">
          {tools.map(({ id, label, icon: Icon }) => (
            <button key={id} type="button" onClick={() => setTool(id)} aria-pressed={tool === id}
              className={`inline-flex min-h-10 items-center gap-2 rounded-lg px-3 text-sm font-semibold ${tool === id ? 'bg-clinic-green text-white' : 'bg-slate-100 text-slate-700'}`}>
              <Icon size={16} />{label}
            </button>
          ))}
          <button type="button" onClick={() => { activeStrokeRef.current = null; activePointerIdRef.current = null; setStrokes((current) => current.slice(0, -1)); }}
            className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-slate-100 px-3 text-sm font-semibold text-slate-700"><RotateCcw size={16} />Undo</button>
          <button type="button" onClick={() => { activeStrokeRef.current = null; activePointerIdRef.current = null; setStrokes([]); }}
            className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-rose-50 px-3 text-sm font-semibold text-rose-700"><Trash2 size={16} />Clear</button>
        </div>
        <div className="flex gap-2">
          <button type="button" onClick={onClose} className="inline-flex min-h-10 items-center gap-2 rounded-lg border border-slate-200 px-3 text-sm font-semibold text-slate-700"><X size={16} />Close</button>
          <button type="button" disabled={saving} onClick={() => void savePrescription()} className="inline-flex min-h-10 items-center gap-2 rounded-lg bg-clinic-green px-4 text-sm font-semibold text-white disabled:opacity-60"><Save size={16} />{saving ? 'Saving…' : 'Save Handwritten Rx'}</button>
        </div>
      </header>
      <div className="mx-auto min-h-0 w-full max-w-6xl flex-1 overflow-y-auto overscroll-contain rounded-b-2xl px-1 touch-pan-y sm:px-4">
        <PrescriptionPage
          pageRef={pageRef}
          canvasRef={canvasRef}
          patientName={patientName}
          patientAge={patientAge}
          prescriptionDate={prescriptionDate}
          onPointerDown={pointerDown}
          onPointerMove={pointerMove}
          onPointerUp={pointerUp}
        />
      </div>
      {error && <p role="alert" className="mx-auto w-full max-w-6xl shrink-0 rounded-b-xl bg-rose-50 px-4 py-2 text-sm font-medium text-rose-700">{error}</p>}
    </div>
  );
}

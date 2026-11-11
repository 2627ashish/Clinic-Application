import { Plus, Trash2 } from 'lucide-react';
import type { BillItem } from '../../types';

interface BillEditorProps {
  items: BillItem[];
  paidAmount: string;
  onItemsChange: (items: BillItem[]) => void;
  onPaidAmountChange: (value: string) => void;
}

export default function BillEditor({ items, paidAmount, onItemsChange, onPaidAmountChange }: BillEditorProps) {
  const total = items.reduce((sum, item) => sum + (Number(item.amount) || 0), 0);
  const paid = Number(paidAmount) || 0;
  const balance = Math.max(total - paid, 0);

  return (
    <section className="sm:col-span-2" aria-labelledby="bill-heading">
      <div className="flex items-center justify-between gap-3">
        <h3 id="bill-heading" className="text-sm font-semibold text-clinic-ink">Bill items</h3>
        <button type="button" onClick={() => onItemsChange([...items, { description: '', amount: 0 }])}
          className="inline-flex items-center gap-1 rounded-lg bg-clinic-mint px-3 py-2 text-xs font-bold text-clinic-green">
          <Plus size={15} /> Add item
        </button>
      </div>
      <div className="mt-2 overflow-hidden rounded-xl border border-slate-200">
        <table className="w-full text-left text-sm">
          <thead className="bg-clinic-paper text-xs text-slate-600">
            <tr><th className="p-3">Description</th><th className="w-36 p-3">Cost (INR)</th><th className="w-12 p-3"><span className="sr-only">Remove</span></th></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {items.map((item, index) => (
              <tr key={index}>
                <td className="p-2">
                  <input required aria-label={`Bill item ${index + 1} description`} value={item.description}
                    onChange={(event) => onItemsChange(items.map((row, i) => i === index ? { ...row, description: event.target.value } : row))}
                    className="w-full rounded-lg border border-slate-200 px-3 py-2" placeholder="e.g. RCT Sitting 1" />
                </td>
                <td className="p-2">
                  <input required min="0" step="0.01" type="number" aria-label={`Bill item ${index + 1} cost`} value={item.amount}
                    onChange={(event) => onItemsChange(items.map((row, i) => i === index ? { ...row, amount: Number(event.target.value) } : row))}
                    className="w-full rounded-lg border border-slate-200 px-3 py-2" />
                </td>
                <td className="p-2">
                  <button type="button" aria-label={`Remove bill item ${index + 1}`}
                    onClick={() => onItemsChange(items.filter((_, row) => row !== index))}
                    className="rounded-lg p-2 text-rose-600 hover:bg-rose-50"><Trash2 size={16} /></button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="mt-3 grid gap-3 rounded-xl bg-clinic-paper p-3 text-sm sm:grid-cols-3">
        <p>Total amount <strong className="ml-1">₹{total.toFixed(2)}</strong></p>
        <label className="font-medium">Paid amount
          <input required min="0" max={total} step="0.01" type="number" value={paidAmount}
            onChange={(event) => onPaidAmountChange(event.target.value)}
            className="ml-2 w-28 rounded-lg border border-slate-200 px-2 py-1" />
        </label>
        <p>Balance due <strong className="ml-1">₹{balance.toFixed(2)}</strong></p>
      </div>
    </section>
  );
}

import { useEffect, useMemo, useState } from 'react';
import { ImagePlus, Send, Sparkles, X } from 'lucide-react';
import { getCampaignEligibleCount, launchCampaignBroadcast } from '../api/client';

const MAX_IMAGE_BYTES = 1024 * 1024;

export default function BroadcastCard() {
  const [campaignTitle, setCampaignTitle] = useState('');
  const [message, setMessage] = useState('');
  const [image, setImage] = useState<File | null>(null);
  const [eligibleCount, setEligibleCount] = useState<number | null>(null);
  const [reviewing, setReviewing] = useState(false);
  const [sending, setSending] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const imagePreview = useMemo(() => image ? URL.createObjectURL(image) : '', [image]);

  useEffect(() => () => {
    if (imagePreview) URL.revokeObjectURL(imagePreview);
  }, [imagePreview]);

  function chooseImage(file: File | undefined) {
    setError('');
    setNotice('');
    if (!file) {
      setImage(null);
      return;
    }
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      setImage(null);
      setError('Choose a JPEG or PNG image. Animated GIFs are not supported by WhatsApp image messages.');
      return;
    }
    if (file.size > MAX_IMAGE_BYTES) {
      setImage(null);
      setError('Images must be 1 MB or smaller.');
      return;
    }
    setImage(file);
  }

  async function reviewCampaign() {
    setError('');
    setNotice('');
    try {
      setEligibleCount(await getCampaignEligibleCount());
      setReviewing(true);
    } catch {
      setError('Could not check the opted-in recipient count. Please try again.');
    }
  }

  async function sendCampaign() {
    setSending(true);
    setError('');
    setNotice('');
    try {
      await launchCampaignBroadcast({ campaignTitle: campaignTitle.trim(), message: message.trim() }, image);
      setNotice(`Campaign queued for ${eligibleCount ?? 0} opted-in patient${eligibleCount === 1 ? '' : 's'}.`);
      setCampaignTitle('');
      setMessage('');
      setImage(null);
      setReviewing(false);
      setEligibleCount(null);
    } catch {
      setError('The campaign could not be queued. Check the message and image, then try again.');
    } finally {
      setSending(false);
    }
  }

  return (
    <section className="overflow-hidden rounded-3xl bg-clinic-ink p-6 text-white shadow-soft sm:p-7">
      <div className="flex items-start gap-4">
        <div className="min-w-0 flex-1">
          <span className="inline-flex items-center gap-1.5 rounded-full bg-white/10 px-3 py-1 text-xs font-semibold text-emerald-100"><Sparkles size={13} /> Patient connection</span>
          <h2 className="mt-4 text-xl font-bold">WhatsApp campaign</h2>
          <p className="mt-1 max-w-2xl text-sm leading-6 text-white/70">Write a message for an occasion, clinic update, or greeting. It will only go to patients who opted in.</p>
        </div>
        <span className="hidden h-12 w-12 place-items-center rounded-2xl bg-white/10 sm:grid"><Sparkles className="text-amber-300" size={23} /></span>
      </div>

      <div className="mt-6 space-y-4 rounded-2xl border border-white/10 bg-white/5 p-4">
        <label className="block text-sm font-semibold">
          Campaign name
          <input value={campaignTitle} onChange={(event) => setCampaignTitle(event.target.value)} maxLength={100}
            className="mt-1.5 w-full rounded-xl border border-white/20 bg-white px-3 py-2.5 font-normal text-clinic-ink outline-none focus:ring-2 focus:ring-emerald-300" placeholder="e.g. Clinic holiday hours" />
        </label>
        <label className="block text-sm font-semibold">
          WhatsApp message
          <textarea value={message} onChange={(event) => setMessage(event.target.value)} maxLength={1024} rows={4}
            className="mt-1.5 w-full rounded-xl border border-white/20 bg-white px-3 py-2.5 font-normal text-clinic-ink outline-none focus:ring-2 focus:ring-emerald-300" placeholder="Write the message patients will receive…" />
          <span className="mt-1 block text-right text-xs text-white/60">{message.length}/1024</span>
        </label>
        <div>
          <label className="inline-flex cursor-pointer items-center gap-2 rounded-xl border border-white/20 px-4 py-2.5 text-sm font-semibold hover:bg-white/10">
            <ImagePlus size={17} /> Add JPEG/PNG image
            <input type="file" accept="image/jpeg,image/png" className="sr-only" onChange={(event) => chooseImage(event.target.files?.[0])} />
          </label>
          {image && (
            <div className="mt-3 flex items-start gap-3 rounded-xl bg-white/10 p-3">
              <img src={imagePreview} alt="Campaign image preview" className="h-20 w-20 rounded-lg object-cover" />
              <div className="min-w-0 flex-1 text-sm"><p className="truncate font-semibold">{image.name}</p><p className="text-white/60">{Math.ceil(image.size / 1024)} KB</p></div>
              <button type="button" onClick={() => setImage(null)} aria-label="Remove campaign image" className="rounded-lg p-2 hover:bg-white/10"><X size={16} /></button>
            </div>
          )}
        </div>

        <p className="text-xs leading-5 text-white/65">Only opted-in recipients are included. Patients can reply STOP to opt out. WhatsApp may require an approved template for messages sent outside its 24-hour customer-service window.</p>
        {error && <p role="alert" className="rounded-lg bg-red-500/15 px-3 py-2 text-sm text-red-100">{error}</p>}
        {notice && <p role="status" className="rounded-lg bg-emerald-500/15 px-3 py-2 text-sm text-emerald-100">{notice}</p>}

        {!reviewing ? (
          <button type="button" disabled={!campaignTitle.trim() || !message.trim()} onClick={() => void reviewCampaign()}
            className="inline-flex min-h-11 items-center justify-center gap-2 rounded-xl bg-white px-4 py-2.5 text-sm font-bold text-clinic-ink transition hover:bg-emerald-50 disabled:cursor-not-allowed disabled:opacity-50">
            Review before sending
          </button>
        ) : (
          <div className="rounded-xl border border-amber-200/30 bg-amber-300/10 p-4">
            <p className="font-semibold">Confirm campaign</p>
            <p className="mt-1 text-sm text-white/80">“{campaignTitle}” will be queued for {eligibleCount} opted-in patient{eligibleCount === 1 ? '' : 's'}{image ? ', with the selected image' : ''}. Review your message above before proceeding.</p>
            {eligibleCount === 0 && <p className="mt-2 text-sm text-amber-100">No patients are currently opted in, so no message will be sent.</p>}
            <div className="mt-4 flex flex-wrap gap-2">
              <button type="button" disabled={sending || eligibleCount === 0} onClick={() => void sendCampaign()}
                className="inline-flex min-h-11 items-center gap-2 rounded-xl bg-white px-4 py-2.5 text-sm font-bold text-clinic-ink disabled:opacity-50">
                <Send size={15} /> {sending ? 'Queueing…' : 'Confirm & send'}
              </button>
              <button type="button" disabled={sending} onClick={() => setReviewing(false)} className="min-h-11 rounded-xl border border-white/25 px-4 py-2.5 text-sm font-semibold hover:bg-white/10">Go back</button>
            </div>
          </div>
        )}
      </div>
    </section>
  );
}

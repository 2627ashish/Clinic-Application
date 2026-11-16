import { useEffect, useState } from 'react';
import { ExternalLink, Play, Youtube } from 'lucide-react';
import { getClinicConfig } from '../../api/client';

const videos = [
  { title: 'Digital dental scanning', videoId: '6p5hTYj1_yY', description: 'See how a full-mouth digital scan supports treatment planning.' },
  { title: 'Wisdom tooth extraction', videoId: 'kvZHJkWN6f0', description: 'A patient shares their experience with wisdom tooth care.' },
  { title: 'Full-mouth digital scan', videoId: 'w1g_uHZ1D0A', description: 'Learn about the clinic’s digital scanning process.' },
];

const channelUrl = 'https://www.youtube.com/@chauhandentalclinic7249';

export default function DoctorVideoShowcase() {
  const [videoCallNumber, setVideoCallNumber] = useState('');
  const [configError, setConfigError] = useState('');
  const [playingVideoId, setPlayingVideoId] = useState<string | null>(null);

  useEffect(() => {
    void getClinicConfig()
      .then((config) => {
        setVideoCallNumber(config.videoCallNumber.replace(/\D/g, ''));
        setConfigError('');
      })
      .catch(() => setConfigError('The WhatsApp video-call helpline is temporarily unavailable.'));
  }, []);

  return (
    <section className="bg-white py-12 sm:py-16" aria-labelledby="doctor-videos-title">
      <div className="mx-auto max-w-7xl px-5 sm:px-8">
        <div className="flex flex-col justify-between gap-5 sm:flex-row sm:items-end">
          <div className="max-w-2xl">
            <p className="text-sm font-bold uppercase tracking-[0.16em] text-clinic-green">Learn from your dentist</p>
            <h2 id="doctor-videos-title" className="mt-2 text-3xl font-bold tracking-tight text-clinic-ink">Dental care videos</h2>
            <p className="mt-2 text-sm text-slate-600">Browse Dr. B.S. Chauhan’s educational videos on YouTube.</p>
          </div>
          <a href={channelUrl} target="_blank" rel="noreferrer"
            className="inline-flex min-h-12 items-center justify-center gap-2 rounded-xl bg-rose-600 px-5 font-semibold text-white hover:bg-rose-700">
            <Youtube size={19} /> Subscribe for Dental Care Tips <ExternalLink size={15} />
          </a>
        </div>
        <div className="mt-7 grid gap-5 md:grid-cols-2 lg:grid-cols-3">
          {videos.map(({ title, videoId, description }) => {
            return (
              <article key={videoId} className="group overflow-hidden rounded-2xl border border-slate-100 bg-white shadow-sm transition duration-200 hover:-translate-y-1 hover:shadow-xl">
                <div className="relative aspect-video overflow-hidden bg-clinic-ink">
                  {playingVideoId === videoId ? (
                    <iframe
                      src={`https://www.youtube-nocookie.com/embed/${videoId}?autoplay=1&rel=0`}
                      title={title}
                      className="absolute inset-0 h-full w-full"
                      allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                      referrerPolicy="strict-origin-when-cross-origin"
                      allowFullScreen
                    />
                  ) : (
                    <button type="button" onClick={() => setPlayingVideoId(videoId)} aria-label={`Play ${title}`}
                      className="absolute inset-0 grid h-full w-full place-items-center">
                      <img src={`https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`} alt="" loading="lazy" className="absolute inset-0 h-full w-full object-cover opacity-85 transition duration-300 group-hover:scale-105" />
                      <span className="relative grid h-14 w-14 place-items-center rounded-full bg-white text-clinic-green shadow-lg transition group-hover:scale-110"><Play size={22} fill="currentColor" /></span>
                    </button>
                  )}
                </div>
                <div className="p-5">
                  <p className="text-xs font-bold uppercase tracking-wider text-clinic-green">Dr. B.S. Chauhan · Dental Care</p>
                  <h3 className="mt-2 text-lg font-bold text-clinic-ink">{title}</h3>
                  <p className="mt-1 text-sm leading-5 text-slate-600">{description}</p>
                </div>
              </article>
            );
          })}
        </div>
        {videoCallNumber && (
          <a href={`https://wa.me/${videoCallNumber}?text=${encodeURIComponent('Hello Dr. Chauhan, I would like to enquire about a WhatsApp video consultation.')}`}
            target="_blank" rel="noreferrer" className="mt-6 inline-flex min-h-11 items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-4 font-semibold text-emerald-800">
            WhatsApp Video Call Helpline
          </a>
        )}
        {configError && <p role="status" className="mt-4 text-sm text-amber-700">{configError}</p>}
      </div>
    </section>
  );
}

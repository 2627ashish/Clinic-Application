export const facilities = [
  'Computer dental treatment',
  'Dental X-ray (R.V.G)',
  'Root canal treatment',
  'Silver filling',
  'Tooth-coloured filling',
  'Cosmetic treatment',
  'Teeth cleaning & polishing',
  'Normal & surgical extraction',
  'Dentures',
  'Crown & bridge',
  'Orthodontic treatment',
  'Jaw fracture treatment',
  'Dental surgery',
];

export function drawPrescriptionPadTemplate(
  context: CanvasRenderingContext2D,
  width: number,
  height: number,
  patientName: string,
  patientAge: number | null,
  prescriptionDate: Date,
) {
  const headerHeight = 170;
  const detailsHeight = 62;
  const footerHeight = 100;
  const sidebarWidth = Math.max(130, Math.round(width * 0.21));
  const bodyTop = headerHeight + detailsHeight;
  const bodyBottom = height - footerHeight;

  context.fillStyle = '#155e52';
  context.fillRect(0, 0, width, headerHeight);
  context.fillStyle = '#d9edaa';
  context.fillRect(0, headerHeight - 42, width, 42);
  context.textAlign = 'center';
  context.fillStyle = '#fff';
  context.font = 'bold 30px sans-serif';
  context.fillText('CHAUHAN DENTAL CLINIC', width / 2, 44, width - 32);
  context.fillStyle = '#7f1d1d';
  context.font = 'bold 18px sans-serif';
  context.fillText('DR. B.S. CHAUHAN (डॉ. बी. एस. चौहान)', width * 0.32, 96, width * 0.55);
  context.font = '12px sans-serif';
  context.fillText('B.D.S (Noida) · Expert Dental Surgeon · Reg: A-5651', width * 0.32, 117, width * 0.58);
  context.fillStyle = '#713f12';
  context.font = 'bold 11px sans-serif';
  context.fillText('Clinic 1 · Sec-82, Noida', width * 0.78, 91, width * 0.38);
  context.fillText('Clinic 2 · Sec-44, Chhalera, Noida', width * 0.78, 119, width * 0.4);

  context.fillStyle = '#f1f5f9';
  context.fillRect(0, headerHeight, width, detailsHeight);
  context.textAlign = 'left';
  context.fillStyle = '#111827';
  context.font = 'bold 12px sans-serif';
  context.fillText(`Name: ${patientName}`, 18, headerHeight + 25, width * 0.52);
  context.fillText(`Age: ${patientAge == null ? 'Not recorded' : `${patientAge} years`}`, width * 0.62, headerHeight + 25, width * 0.34);
  context.fillText(`Date: ${new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium' }).format(prescriptionDate)}`, 18, headerHeight + 48, width - 36);

  context.fillStyle = '#f8fafc';
  context.fillRect(0, bodyTop, sidebarWidth, bodyBottom - bodyTop);
  context.fillStyle = '#7f1d1d';
  context.font = 'bold 14px sans-serif';
  context.fillText('FACILITIES', 12, bodyTop + 26, sidebarWidth - 24);
  context.fillStyle = '#111827';
  context.font = '10px sans-serif';
  facilities.forEach((facility, index) => {
    context.fillText(`• ${facility}`, 12, bodyTop + 52 + index * 23, sidebarWidth - 22);
  });
  context.fillStyle = '#155e52';
  context.font = 'bold 24px sans-serif';
  context.fillText('Rx', sidebarWidth + 18, bodyTop + 32);

  context.fillStyle = '#155e52';
  context.fillRect(0, bodyBottom, width, footerHeight);
  context.fillStyle = '#fff';
  context.font = 'bold 12px sans-serif';
  context.fillText('“Your smile, our commitment”', 18, bodyBottom + 25);
  context.font = '10px sans-serif';
  context.fillText('Clinic 1 · Sec-82, Madhuban Apartments, Noida', 18, bodyBottom + 50, width - 36);
  context.fillText('Clinic 2 · Sec-44, Chhalera, Noida', 18, bodyBottom + 70, width - 36);
}

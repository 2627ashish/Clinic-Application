export type ClinicLocation = 'SEC_44' | 'SEC_82';
export const clinicLocations: ClinicLocation[] = ['SEC_44', 'SEC_82'];

export function isClinicLocation(value: string): value is ClinicLocation {
  return value === 'SEC_44' || value === 'SEC_82';
}

export const clinicDetails: Record<ClinicLocation, {
  label: string;
  address: string;
  hours: string;
  directions: string;
}> = {
  SEC_44: {
    label: 'Clinic 1 · Sec-44, Chhalera',
    address: 'Gali No. 3, Chhalera, Sec-44, Noida',
    hours: 'Mon–Sat: 10:00 AM–1:00 PM · 5:30 PM–9:00 PM',
    directions: 'https://www.google.com/maps/place/28%C2%B033\'28.3%22N+77%C2%B020\'38.1%22E/@28.5578734,77.3439285,17z/data=!3m1!4b1!4m4!3m3!8m2!3d28.5578734!4d77.3439285?entry=ttu&g_ep=EgoyMDI2MTEwNC4wIKXMDSoASAFQAw%3D%3D',
  },
  SEC_82: {
    label: 'Clinic 2 · Sec-82, Madhuban',
    address: '78A MIG, Madhuban Apartments, Sec-82, Noida',
    hours: 'Mon–Sat: 8:30 AM–10:00 AM · 2:00 PM–5:00 PM',
    directions: 'https://www.google.com/maps/place/78,+Swarnim+Vihar,+Sector+82,+Noida,+Uttar+Pradesh+201304/@28.5296124,77.3844855,17z/data=!3m1!4b1!4m6!3m5!1s0x390ce8a377ea0455:0xdf316b5ab3199f24!8m2!3d28.5296124!4d77.3870604!16s%2Fg%2F11wb219d5k?entry=ttu&g_ep=EgoyMDI2MTEwNC4wIKXMDSoASAFQAw%3D%3D',
  },
};

const clinicShifts: Record<ClinicLocation, [number, number][]> = {
  SEC_44: [[600, 780], [1050, 1260]],
  SEC_82: [[510, 600], [840, 1020]],
};

export function clinicSlotMinutes(location: ClinicLocation): number[] {
  return clinicShifts[location].flatMap(([start, end]) =>
    Array.from({ length: (end - start) / 30 + 1 }, (_, index) => start + index * 30));
}

export function normalizeIndianPhone(value: string): string | null {
  const digits = value.replace(/\D/g, '');
  const mobile = digits.length === 10
    ? digits
    : digits.length === 11 && digits.startsWith('0')
      ? digits.slice(1)
      : digits.length === 12 && digits.startsWith('91')
        ? digits.slice(2)
        : '';
  return /^[6-9]\d{9}$/.test(mobile) ? `91${mobile}` : null;
}

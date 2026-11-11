import type { ClinicLocation } from '../utils/clinic';

export type ConsultationType = 'IN_CLINIC' | 'ONLINE';

export interface AiIntakePayload {
  patientName: string;
  phoneNumber: string;
  age: number;
  gender: 'Male' | 'Female' | 'Other';
  preferredTime: string;
  clinicLocation: ClinicLocation;
  symptomsDescription: string;
  consultationType: ConsultationType;
  whatsappMarketingOptIn: boolean;
}

export interface AppointmentResponse {
  appointmentId: string;
  patientName: string;
  phoneNumber: string;
  appointmentTime: string;
  clinicLocation: ClinicLocation;
  consultationType: ConsultationType;
  meetingLink: string | null;
  status: string;
  chiefComplaintSummary: string;
  createdAt: string;
}

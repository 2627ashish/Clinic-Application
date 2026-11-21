import type { ClinicLocation } from '../utils/clinic';

export type ConsultationType = 'IN_CLINIC' | 'ONLINE';
export type AppointmentStatus =
  | 'PENDING_DOCTOR_APPROVAL'
  | 'CONFIRMED'
  | 'RESCHEDULED'
  | 'COMPLETED'
  | 'CANCELLED';

export interface MedicalDeclaration {
  hasChronicIllness: boolean;
  takingLongTermMedication: boolean;
  lignocaineConsent: boolean;
  consentToTreatment: boolean;
  adherenceConsent: boolean;
  hadPreviousDentalTreatment: boolean;
  rctCommitment: boolean;
  isPregnant: boolean | null;
  isBreastfeeding: boolean | null;
  illnessDetails: string;
  medicationDetails: string;
}

export interface AiIntakePayload {
  patientName: string;
  phoneNumber: string;
  dateOfBirth: string;
  gender: 'Male' | 'Female' | 'Other';
  preferredTime: string;
  clinicLocation: ClinicLocation;
  symptomsDescription: string;
  consultationType: ConsultationType;
  whatsappMarketingOptIn: boolean;
  medicalHistory: string[];
  ongoingTreatments: string;
  isPregnant: boolean | null;
  medicalDeclaration: MedicalDeclaration;
}

export interface AppointmentResponse {
  id: string;
  patientId: string;
  patientName: string;
  phoneNumber: string;
  dateOfBirth: string | null;
  patientAge: number | null;
  gender: string | null;
  appointmentTime: string;
  clinicLocation: ClinicLocation;
  consultationType: ConsultationType;
  meetingLink: string | null;
  status: AppointmentStatus;
  chiefComplaintSummary: string;
  doctorNotes: string | null;
  medicalHistory: string[] | null;
  ongoingTreatments: string | null;
  isPregnant: boolean | null;
  medicalDeclaration: MedicalDeclaration | null;
  intakeSource: string | null;
  quotedFee: number | null;
  createdAt: string;
}

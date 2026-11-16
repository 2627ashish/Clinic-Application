import type { ClinicLocation } from '../utils/clinic';
import type { ConsultationType } from './appointment';

export type AppointmentStatus =
  | 'PENDING_DOCTOR_APPROVAL'
  | 'CONFIRMED'
  | 'RESCHEDULED'
  | 'COMPLETED'
  | 'CANCELLED';

export interface Patient {
  id: string;
  fullName: string;
  phoneNumber: string;
  age: number | null;
  gender: string | null;
  createdAt: string;
}

export interface Appointment {
  id: string;
  patientId: string;
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
  intakeSource: string | null;
  quotedFee: number | null;
}

export interface AppointmentView extends Appointment {
  patientName: string;
  phoneNumber: string;
}

export interface PrescriptionItem {
  name: string;
  dosage: string;
  duration: string;
  instructions: string;
}

export interface BillItem {
  description: string;
  amount: number;
}

export interface TreatmentAndBill {
  id: string;
  appointmentId: string;
  patientId: string;
  symptomsAndDiagnosis: string;
  procedurePerformed: string;
  medicines: PrescriptionItem[];
  aftercareGuidelines: string;
  billItems: BillItem[];
  totalAmount: number;
  paidAmount: number;
  createdAt: string;
}

export interface AppointmentStatusUpdateRequest {
  status: AppointmentStatus;
  rescheduledTime?: string;
}

export interface ConsultationCompleteRequest {
  symptomsAndDiagnosis: string;
  procedurePerformed: string;
  medicines: PrescriptionItem[];
  aftercareGuidelines: string;
  billItems: BillItem[];
  paidAmount: number;
}

export type PatientDocumentCategory =
  | 'XRAY_RVG'
  | 'EXTERNAL_PRESCRIPTION'
  | 'LAB_REPORT'
  | 'OTHER'
  | 'HANDWRITTEN_RX';

export interface PatientDocumentView {
  id: string;
  appointmentId: string;
  fileName: string;
  contentType: string;
  category: PatientDocumentCategory;
  createdAt: string;
}

export interface WalkInRequest {
  patientName: string;
  phoneNumber: string;
  age: number;
  gender: 'Male' | 'Female' | 'Other';
  clinicLocation: ClinicLocation;
  source: 'PHONE_CALL' | 'DIRECT_WALK_IN';
  quotedFee: number | null;
  notes: string;
}

export interface ClinicConfig {
  videoCallNumber: string;
}

export interface BroadcastRequest {
  campaignTitle: string;
  message: string;
}

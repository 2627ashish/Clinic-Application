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

export interface BroadcastRequest {
  campaignTitle: string;
  message: string;
}

import axios from 'axios';
import type {
  AppointmentStatusUpdateRequest,
  AppointmentView,
  BroadcastRequest,
  ConsultationCompleteRequest,
  TreatmentAndBill,
  PatientDocumentView,
  PatientDocumentCategory,
  PrescriptionItem,
  WalkInRequest,
  ClinicConfig,
} from '../types';
import type { AppointmentResponse, ConsultationType } from '../types/appointment';

export const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  const url = config.url ?? '';
  const isDoctorEndpoint = url.startsWith('/appointments')
    || url.startsWith('/campaigns')
    || url === '/doctor/auth/password';
  const token = isDoctorEndpoint ? localStorage.getItem('doctor_auth_token') : null;
  if (token && !config.headers.Authorization) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

client.interceptors.response.use((response) => response, (error: unknown) => {
  if (axios.isAxiosError(error)) {
    const url = error.config?.url ?? '';
    const isDoctorEndpoint = url.startsWith('/appointments') || url.startsWith('/campaigns');
    if (isDoctorEndpoint && error.response?.status === 401 && localStorage.getItem('doctor_auth_token')) {
      localStorage.removeItem('doctor_auth_token');
      window.dispatchEvent(new Event('doctor-session-expired'));
    }
  }
  return Promise.reject(error);
});

export interface DoctorLoginResponse {
  token: string;
  passwordChangeRequired: boolean;
}

export interface DoctorSetupStatus {
  setupRequired: boolean;
}

export async function getDoctorSetupStatus(): Promise<DoctorSetupStatus> {
  const response = await client.get<DoctorSetupStatus>('/doctor/auth/setup-status');
  return response.data;
}

export async function setupDoctorAccount(
  username: string,
  password: string,
  fullName: string,
  email: string,
): Promise<DoctorLoginResponse> {
  const response = await client.post<DoctorLoginResponse>('/doctor/auth/setup', {
    username, password, fullName, email,
  });
  return response.data;
}

export interface PatientOtpResponse {
  success: boolean;
  message: string;
  resendCooldown: number;
  mockOtp: string | null;
}

export interface PatientVisitSummary {
  appointmentId: string;
  visitDate: string;
  diagnosis: string;
  procedurePerformed: string;
  medicines: PrescriptionItem[];
  aftercareGuidelines: string | null;
}

export interface PatientProfileView {
  fullName: string;
  phoneNumber: string;
  age: number | null;
  dateOfBirth: string | null;
  gender: string | null;
}

export interface PatientPortalSession {
  token: string;
  patientName: string;
}

export interface PatientAuthResponse {
  patients: PatientPortalSession[];
}

export async function doctorLogin(username: string, password: string): Promise<DoctorLoginResponse> {
  const response = await client.post<DoctorLoginResponse>('/doctor/auth/login', { username, password });
  return response.data;
}

export async function changeDoctorPassword(currentPassword: string, newPassword: string): Promise<void> {
  await client.post('/doctor/auth/password', { currentPassword, newPassword });
}

export async function requestPatientOtp(phoneNumber: string): Promise<PatientOtpResponse> {
  const response = await client.post<PatientOtpResponse>('/auth/patient/request-otp', { phoneNumber });
  return response.data;
}

export async function verifyPatientOtp(phoneNumber: string, code: string): Promise<PatientAuthResponse> {
  const response = await client.post<PatientAuthResponse>('/auth/patient/verify-otp', { phoneNumber, code });
  return response.data;
}

export async function getPatientProfile(token: string): Promise<PatientProfileView> {
  const response = await client.get<PatientProfileView>('/patient-portal/profile', {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
}

export async function getPatientVisits(token: string): Promise<PatientVisitSummary[]> {
  const response = await client.get<PatientVisitSummary[]>('/patient-portal/visits', {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
}

export async function downloadPatientPrescription(token: string, appointmentId: string): Promise<Blob> {
  const response = await client.get<Blob>(
    `/appointments/${encodeURIComponent(appointmentId)}/prescription-pdf`,
    { headers: { Authorization: `Bearer ${token}` }, responseType: 'blob' },
  );
  return response.data;
}

export async function downloadPatientBill(token: string, appointmentId: string): Promise<Blob> {
  const response = await client.get<Blob>(
    `/appointments/${encodeURIComponent(appointmentId)}/bill-pdf`,
    { headers: { Authorization: `Bearer ${token}` }, responseType: 'blob' },
  );
  return response.data;
}

export async function downloadPatientHandwrittenRx(token: string, appointmentId: string): Promise<Blob> {
  const response = await client.get<Blob>(
    `/appointments/${encodeURIComponent(appointmentId)}/handwritten-rx/download`,
    { headers: { Authorization: `Bearer ${token}` }, responseType: 'blob' },
  );
  return response.data;
}

export async function getPatientDocuments(token: string): Promise<PatientDocumentView[]> {
  const response = await client.get<PatientDocumentView[]>('/patient-portal/documents', {
    headers: { Authorization: `Bearer ${token}` },
  });
  return response.data;
}

export async function downloadPatientDocument(token: string, documentId: string): Promise<Blob> {
  const response = await client.get<Blob>(
    `/patient-portal/documents/${encodeURIComponent(documentId)}/download`,
    { headers: { Authorization: `Bearer ${token}` }, responseType: 'blob' },
  );
  return response.data;
}

export async function createWalkIn(request: WalkInRequest): Promise<{ id: string }> {
  const response = await client.post<{ id: string }>('/appointments/walk-ins', request);
  return response.data;
}

export async function uploadAppointmentDocument(
  appointmentId: string,
  file: File,
  category: PatientDocumentCategory,
): Promise<PatientDocumentView> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('category', category);
  const response = await client.post<PatientDocumentView>(
    `/appointments/${encodeURIComponent(appointmentId)}/documents`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );
  return response.data;
}

export async function uploadHandwrittenPrescription(
  appointmentId: string,
  file: Blob,
): Promise<PatientDocumentView> {
  const formData = new FormData();
  formData.append('file', file, 'handwritten-prescription.png');
  const response = await client.post<PatientDocumentView>(
    `/appointments/${encodeURIComponent(appointmentId)}/handwritten-rx`,
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );
  return response.data;
}

export async function getPatientVisitHistory(appointmentId: string): Promise<TreatmentAndBill[]> {
  const response = await client.get<TreatmentAndBill[]>(
    `/appointments/${encodeURIComponent(appointmentId)}/history`,
  );
  return response.data;
}

export async function getClinicConfig(): Promise<ClinicConfig> {
  const response = await client.get<ClinicConfig>('/clinic/config');
  return response.data;
}

export interface AppointmentPage {
  content: AppointmentView[];
  totalPages: number;
  totalElements: number;
  number: number;
}

export async function getAppointments(
  page = 0,
  size = 10,
  type?: ConsultationType,
  status?: string,
): Promise<AppointmentPage> {
  const response = await client.get<{
    content: AppointmentResponse[];
    totalPages: number;
    totalElements: number;
    number: number;
  }>('/appointments', { params: { page, size, ...(type ? { type } : {}), ...(status ? { status } : {}) } });
  return { ...response.data, content: response.data.content };
}

export async function deleteAppointment(id: string): Promise<void> {
  await client.delete(`/appointments/${encodeURIComponent(id)}`);
}

export async function updateAppointmentStatus(
  id: string,
  request: AppointmentStatusUpdateRequest,
): Promise<void> {
  await client.patch(`/appointments/${encodeURIComponent(id)}/status`, request);
}

export async function completeVisit(
  id: string,
  request: ConsultationCompleteRequest,
): Promise<TreatmentAndBill> {
  const response = await client.post<TreatmentAndBill>(
    `/appointments/${encodeURIComponent(id)}/complete-visit`,
    request,
  );
  return response.data;
}

export async function getCampaignEligibleCount(): Promise<number> {
  const response = await client.get<number>('/campaigns/eligible-count');
  return response.data;
}

export async function launchCampaignBroadcast(request: BroadcastRequest, image: File | null): Promise<void> {
  const formData = new FormData();
  formData.append('campaign', new Blob([JSON.stringify(request)], { type: 'application/json' }));
  if (image) formData.append('image', image);
  await client.post('/campaigns/broadcast', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
}

import axios from 'axios';
import type {
  AppointmentStatusUpdateRequest,
  AppointmentView,
  BroadcastRequest,
  ConsultationCompleteRequest,
  TreatmentAndBill,
} from '../types';

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
  message: string;
  demoCode: string | null;
}

export interface PatientVisitSummary {
  appointmentId: string;
  visitDate: string;
  diagnosis: string;
  procedurePerformed: string;
}

export interface PatientPortalSession {
  token: string;
  patientName: string;
}

export async function doctorLogin(username: string, password: string): Promise<DoctorLoginResponse> {
  const response = await client.post<DoctorLoginResponse>('/doctor/auth/login', { username, password });
  return response.data;
}

export async function changeDoctorPassword(currentPassword: string, newPassword: string): Promise<void> {
  await client.post('/doctor/auth/password', { currentPassword, newPassword });
}

export async function requestPatientOtp(phoneNumber: string): Promise<PatientOtpResponse> {
  const response = await client.post<PatientOtpResponse>('/patient-portal/otp', { phoneNumber });
  return response.data;
}

export async function verifyPatientOtp(phoneNumber: string, code: string): Promise<PatientPortalSession> {
  const response = await client.post<PatientPortalSession>('/patient-portal/verify', { phoneNumber, code });
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
    `/patient-portal/visits/${encodeURIComponent(appointmentId)}/pdf`,
    { headers: { Authorization: `Bearer ${token}` }, responseType: 'blob' },
  );
  return response.data;
}

export async function getAppointments(): Promise<AppointmentView[]> {
  const response = await client.get<AppointmentView[]>('/appointments');
  return response.data;
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

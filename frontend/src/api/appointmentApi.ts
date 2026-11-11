import { client } from './client';
import type { AiIntakePayload, AppointmentResponse } from '../types/appointment';

export async function submitAiIntake(data: AiIntakePayload): Promise<AppointmentResponse> {
  const response = await client.post<AppointmentResponse>('/appointments/ai-intake', data);
  return response.data;
}

export async function fetchPendingAppointments(): Promise<AppointmentResponse[]> {
  const response = await client.get<AppointmentResponse[]>('/appointments/pending');
  return response.data;
}

package com.chaula.dental.service;

import com.chaula.dental.dto.AiIntakeRequest;
import com.chaula.dental.dto.AppointmentResponse;

import java.util.List;

public interface AppointmentIntakeService {
    AppointmentResponse processAiIntake(AiIntakeRequest request);

    List<AppointmentResponse> findPendingAppointments();
}

package com.chaula.dental.service;

import com.chaula.dental.domain.PatientOtp;
import com.chaula.dental.dto.AuthResponse;
import com.chaula.dental.dto.OtpResponse;
import com.chaula.dental.messaging.event.WhatsAppOutboundMessage;
import com.chaula.dental.repository.PatientOtpRepository;
import com.chaula.dental.util.PhoneUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class PatientAuthServiceImpl implements PatientAuthService {
    private static final long OTP_LIFETIME_SECONDS = 600;
    private static final int RESEND_COOLDOWN_SECONDS = 30;
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final String OTP_SENT_MESSAGE = "OTP sent to WhatsApp";

    private final PatientOtpRepository otpRepository;
    private final PatientPortalService patientPortalService;
    private final WhatsAppDirectMessageSender whatsAppSender;
    private final SecureRandom secureRandom = new SecureRandom();
    private final boolean mockOtp;

    public PatientAuthServiceImpl(
            PatientOtpRepository otpRepository,
            PatientPortalService patientPortalService,
            WhatsAppDirectMessageSender whatsAppSender,
            @Value("${patient.portal.mock-otp:false}") boolean mockOtp
    ) {
        this.otpRepository = otpRepository;
        this.patientPortalService = patientPortalService;
        this.whatsAppSender = whatsAppSender;
        this.mockOtp = mockOtp;
    }

    @Override
    public OtpResponse requestOtp(String rawPhone) {
        String phoneNumber = normalize(rawPhone);
        Instant now = Instant.now();
        Optional<PatientOtp> existing = otpRepository.findByPhoneNumber(phoneNumber);
        if (existing.filter(otp -> isActive(otp, now))
                .filter(otp -> otp.getLastSentAt() != null
                        && Duration.between(otp.getLastSentAt(), now).getSeconds() < RESEND_COOLDOWN_SECONDS)
                .isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Please wait 30 seconds before requesting a new OTP."
            );
        }

        String otpCode = String.format(Locale.ROOT, "%04d", secureRandom.nextInt(10_000));
        PatientOtp otp = existing.orElseGet(() -> PatientOtp.builder().phoneNumber(phoneNumber).build());
        otp.setOtpCode(otpCode);
        otp.setVerifyAttempts(0);
        otp.setLastSentAt(now);
        otp.setExpiresAt(now.plusSeconds(OTP_LIFETIME_SECONDS));
        otpRepository.save(otp);

        String message = "🔐 *Chauhan Dental Clinic Verification Code*\n\n"
                + "Your login OTP is: *" + otpCode + "*\n"
                + "Valid for 10 minutes.\n\n"
                + "_If you did not request this, please ignore._";
        whatsAppSender.send(WhatsAppOutboundMessage.text(phoneNumber, message));
        return new OtpResponse(true, OTP_SENT_MESSAGE, RESEND_COOLDOWN_SECONDS, mockOtp ? otpCode : null);
    }

    @Override
    public AuthResponse verifyOtp(String rawPhone, String enteredOtp) {
        String phoneNumber = normalize(rawPhone);
        PatientOtp otp = otpRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> expiredOtp());
        Instant now = Instant.now();
        if (!isActive(otp, now)) {
            otpRepository.deleteByPhoneNumber(phoneNumber);
            throw expiredOtp();
        }

        otp.setVerifyAttempts(otp.getVerifyAttempts() + 1);
        if (otp.getVerifyAttempts() > MAX_VERIFY_ATTEMPTS) {
            otpRepository.deleteByPhoneNumber(phoneNumber);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many failed attempts. Request a new OTP.");
        }
        otpRepository.save(otp);

        if (enteredOtp == null || otp.getOtpCode() == null || !otp.getOtpCode().equals(enteredOtp.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP code. Please try again.");
        }

        otpRepository.deleteByPhoneNumber(phoneNumber);
        return patientPortalService.createSessionsForPhone(phoneNumber);
    }

    private boolean isActive(PatientOtp otp, Instant now) {
        return otp.getExpiresAt() != null && otp.getExpiresAt().isAfter(now);
    }

    private String normalize(String rawPhone) {
        try {
            return PhoneUtils.normalizeIndianPhone(rawPhone);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private ResponseStatusException expiredOtp() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has expired. Please request a new one.");
    }
}

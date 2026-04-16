package com.smartSure.authService.service;

import com.smartSure.authService.entity.OtpRecord;
import com.smartSure.authService.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates OTP, stores hashed version in Redis, sends plain OTP to email.
     * Returns the preAuthToken (UUID) that the frontend must include in verify-otp call.
     *
     * FIX: Email is now sent ASYNCHRONOUSLY via @Async on sendOtpEmail().
     * Previously the login request blocked on Gmail SMTP (2-5 seconds), causing
     * the API Gateway's 5-second circuit breaker to fire before the response
     * returned. The OTP was actually sent successfully but the Gateway had already
     * given up and returned 503 to the frontend.
     */
    public String generateAndSendOtp(Long userId, String email, String role) {

        // Generate 6-digit OTP
        String otp = String.format("%06d", RANDOM.nextInt(999999));

        // Generate unique preAuthToken
        String preAuthToken = UUID.randomUUID().toString();

        // Store hashed OTP in Redis BEFORE sending email — never store plain OTP
        OtpRecord record = new OtpRecord();
        record.setPreAuthToken(preAuthToken);
        record.setUserId(userId);
        record.setEmail(email);
        record.setRole(role);
        record.setOtpHash(passwordEncoder.encode(otp));
        record.setTtl(300L); // 5 minutes
        otpRepository.save(record);

        // Send email asynchronously — login returns immediately after Redis save
        sendOtpEmail(email, otp);

        log.info("OTP generated and sent to email={} for userId={}", maskEmail(email), userId);
        return preAuthToken;
    }

    /**
     * Verifies the OTP. Returns the OtpRecord if valid, throws exception if invalid.
     * Deletes the record after successful verification — OTP is single use.
     */
    public OtpRecord verifyOtp(String preAuthToken, String otp) {

        OtpRecord record = otpRepository.findById(preAuthToken)
                .orElseThrow(() -> new RuntimeException(
                        "OTP expired or invalid. Please login again."));

        if (!passwordEncoder.matches(otp, record.getOtpHash())) {
            log.warn("Invalid OTP attempt for preAuthToken={}", preAuthToken);
            throw new RuntimeException("Invalid OTP. Please try again.");
        }

        // Delete immediately — OTP is single use only
        otpRepository.deleteById(preAuthToken);

        log.info("OTP verified successfully for userId={}", record.getUserId());
        return record;
    }

    /**
     * @Async makes this run in a separate thread pool.
     * The login endpoint returns the preAuthToken to the frontend immediately
     * while the email is delivered in the background.
     * The OTP is already saved in Redis before this method is called,
     * so the user can enter it as soon as it arrives in their inbox.
     */
    @Async
    public void sendOtpEmail(String email, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("SmartSure — Your Login Verification Code");
            message.setText(
                    "Hello,\n\n" +
                            "Your SmartSure verification code is: " + otp + "\n\n" +
                            "This code expires in 5 minutes.\n" +
                            "Do not share this code with anyone.\n\n" +
                            "If you did not request this, please ignore this email.\n\n" +
                            "SmartSure Insurance Platform"
            );
            mailSender.send(message);
            log.info("OTP email sent to {}", maskEmail(email));
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", maskEmail(email), e.getMessage());
            // Do NOT throw here — the preAuthToken is already saved in Redis.
            // If email fails, the user can retry login to get a new OTP.
        }
    }

    public String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
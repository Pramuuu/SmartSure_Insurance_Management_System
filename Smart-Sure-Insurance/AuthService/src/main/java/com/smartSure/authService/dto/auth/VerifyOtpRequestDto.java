package com.smartSure.authService.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequestDto {

    @NotBlank(message = "Pre-auth token is required")
    private String preAuthToken;   // UUID received from step 1

    @NotBlank(message = "OTP is required")
    @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
    private String otp;
}
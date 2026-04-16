package com.smartSure.authService.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PreAuthResponseDto {
    private String preAuthToken;   // UUID sent to frontend
    private String email;          // masked email: pr***@gmail.com
    private String message;        // "OTP sent to your registered email"
}

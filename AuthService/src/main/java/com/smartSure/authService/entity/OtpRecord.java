package com.smartSure.authService.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("otp")
public class OtpRecord {

    @Id
    private String preAuthToken;  // UUID — key in Redis

    private Long userId;
    private String email;
    private String role;
    private String otpHash;       // bcrypt hash of the OTP, never store plain OTP

    @TimeToLive
    private Long ttl = 300L;      // 5 minutes — auto-deleted by Redis after this
}
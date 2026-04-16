package com.smartSure.authService.dto.client;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerProfileResponse {
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Integer age;
}
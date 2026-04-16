package com.smartSure.authService.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(name="Auth Response Dto")
public class AuthResponseDto {
	
	private String token;
	private String email;
	private String role;
}

package com.smartSure.authService.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name="Login Request Dto")
public class LoginRequestDto {
	
	@Email(message = "Invalid email format")
	@NotBlank(message="Email should not be blank")
	private String email;
	
	@NotBlank(message="Password should not be blank")
	@Size(min=8, message="Minimum length of password is 8")
	private String password;
}

package com.smartSure.authService.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name="Admin Create Request Dto")
public class AdminCreateRequestDto {

	@NotBlank(message="First Name should not be blank")
	private String firstName;
	private String lastName;

	@Email(message = "Invalid email format")
	@NotBlank(message="Email should not be blank")
	private String email;

	@NotBlank(message="password should not be blank")
	private String password;
}
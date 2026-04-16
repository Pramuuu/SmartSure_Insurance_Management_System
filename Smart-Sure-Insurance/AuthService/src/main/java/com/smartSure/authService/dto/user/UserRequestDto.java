package com.smartSure.authService.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(name="User Request Dto")
public class UserRequestDto {
	
	@NotBlank(message="First name should not be blank")
	private String firstName;
	private String lastName;
	
	@Email
	@NotBlank(message="Email should not be blank")
	private String email;
	
	@Size(min=8, message="Minimum length of password should be 8")
	@NotBlank(message="Password is needed")
	private String password;
	
	@Min(value = 10, message="Length of the phone number is 10")
	private Long phone;
}

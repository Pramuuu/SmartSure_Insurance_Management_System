package com.smartSure.authService.dto.user;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name="User Response Dto")
public class UserResponseDto {
	
	private Long userId;
	private String firstName;
	private String lastName;
	private String email;
//	private String password;
	private Long phone;
	private String role;
}

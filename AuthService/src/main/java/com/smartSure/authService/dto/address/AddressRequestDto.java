package com.smartSure.authService.dto.address;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(name="Address Request Dto")
public class AddressRequestDto {
	
	private String city;
	private String state;
	private Long zip;
	private String street_address;
}

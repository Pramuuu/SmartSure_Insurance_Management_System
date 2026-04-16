package com.smartSure.claimService.dto;

import lombok.Data;

@Data
public class AddressResponseDto {
    private Long addressId;
    private String city;
    private String state;
    private Long zip;
    private String street_address;
}

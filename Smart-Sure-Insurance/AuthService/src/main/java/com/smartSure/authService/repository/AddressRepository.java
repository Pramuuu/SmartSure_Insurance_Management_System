package com.smartSure.authService.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smartSure.authService.entity.Address;

public interface AddressRepository extends JpaRepository<Address, Long>{

}

package com.smartSure.authService.service;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.smartSure.authService.dto.address.AddressRequestDto;
import com.smartSure.authService.dto.address.AddressResponseDto;
import com.smartSure.authService.entity.Address;
import com.smartSure.authService.entity.User;
import com.smartSure.authService.exception.AddressNotFoundException;
import com.smartSure.authService.exception.UserNotFoundException;
import com.smartSure.authService.repository.AddressRepository;
import com.smartSure.authService.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddressService {
	
	private final AddressRepository repo;
	private final UserRepository uRepo;
	private final ModelMapper modelMapper;
	
	public AddressResponseDto create(AddressRequestDto reqDto, Long userId) {
		
		User user = uRepo.findById(userId).orElseThrow(() -> new UserNotFoundException("User does not exists"));
		Address address = modelMapper.map(reqDto, Address.class);
		user.setAddress(address);
		
		repo.save(address);
		uRepo.save(user);
		
		return modelMapper.map(address, AddressResponseDto.class);
	}
	
	public AddressResponseDto update(AddressRequestDto reqDto, Long userId) {
		
		User user = uRepo.findById(userId).orElseThrow(() -> new UserNotFoundException("User does not exists"));
		Address address = user.getAddress();
		
		if(address == null) throw new AddressNotFoundException("This user does not have any address set yet");
		
		modelMapper.map(reqDto, address);
		user.setAddress(address);
		
		repo.save(address);
		uRepo.save(user);
		
		return modelMapper.map(address, AddressResponseDto.class);
	}
	
	public AddressResponseDto get(Long userId) {
		
		User user = uRepo.findById(userId).orElseThrow(() -> new UserNotFoundException("User does not exists"));
		Address address = user.getAddress();
		
		if(address == null) throw new AddressNotFoundException("This user does not have any address set yet");
		
		return modelMapper.map(address, AddressResponseDto.class);
	}
	
	public AddressResponseDto delete(Long userId) {
		
		User user = uRepo.findById(userId).orElseThrow(() -> new UserNotFoundException("User does not exists"));
		Address address = user.getAddress();
		
		if(address == null) throw new AddressNotFoundException("This user does not have any address set yet");
		
		user.setAddress(null);
		
		repo.deleteById(address.getAddressId());
		uRepo.save(user);
		
		return modelMapper.map(address, AddressResponseDto.class);
	}
}

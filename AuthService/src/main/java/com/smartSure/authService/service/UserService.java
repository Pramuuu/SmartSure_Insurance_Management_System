package com.smartSure.authService.service;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.smartSure.authService.dto.user.UserRequestDto;
import com.smartSure.authService.dto.user.UserResponseDto;
import com.smartSure.authService.entity.User;
import com.smartSure.authService.exception.UserNotFoundException;
import com.smartSure.authService.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
	
	private final UserRepository repo;
	private final ModelMapper modelMapper;
	
	public UserResponseDto add(UserRequestDto reqDto) {
		
		User user = repo.findByEmail(reqDto.getEmail()).orElseThrow(() -> new UserNotFoundException("This email is not registered"));
		modelMapper.map(reqDto, user);
		repo.save(user);
		
		return modelMapper.map(user, UserResponseDto.class);
	}
	
	public UserResponseDto update(UserRequestDto reqDto, Long userId) {
		
		User user = repo.findById(userId).orElseThrow(() -> new UserNotFoundException("This user is not present"));
		modelMapper.map(reqDto, user);
		repo.save(user);
		
		return modelMapper.map(user, UserResponseDto.class);
	}
	
	public UserResponseDto get(Long userId) {
		
		User user = repo.findById(userId).orElseThrow(() -> new UserNotFoundException("This user is not present"));
		
		return modelMapper.map(user, UserResponseDto.class);
	}
	
	public List<UserResponseDto> getAll() {
		return repo.findAll().stream()
				.map(u -> modelMapper.map(u, UserResponseDto.class))
				.collect(Collectors.toList());
	}

	public UserResponseDto delete(Long userId) {
		
		User user = repo.findById(userId).orElseThrow(() -> new UserNotFoundException("This user is not present"));
		repo.deleteById(userId);
		
		return modelMapper.map(user, UserResponseDto.class);
	}
}

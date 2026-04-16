package com.smartSure.authService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.smartSure.authService.entity.Role;
import com.smartSure.authService.entity.User;
import com.smartSure.authService.repository.UserRepository;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync  // Required for @Async on OtpService.sendOtpEmail() to work
public class Application {

	@Value("${admin.bootstrap.password:Admin@SmartSure2024!}")
	private String adminBootstrapPassword;

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public CommandLineRunner bootstrapAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			String adminEmail = "admin@smartsure.com";
			if (userRepository.findByEmail(adminEmail).isEmpty()) {
				User admin = new User();
				admin.setEmail(adminEmail);
				admin.setPassword(passwordEncoder.encode(adminBootstrapPassword));
				admin.setFirstName("Admin");
				admin.setLastName("User");
				admin.setRole(Role.ADMIN);
				userRepository.save(admin);
				System.out.println("Default admin created: " + adminEmail);
			}
		};
	}
}
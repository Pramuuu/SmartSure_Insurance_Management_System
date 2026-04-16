package com.smartSure.authService.service;

import com.smartSure.authService.dto.auth.*;
import com.smartSure.authService.entity.OtpRecord;
import com.smartSure.authService.entity.Role;
import com.smartSure.authService.entity.User;
import com.smartSure.authService.repository.UserRepository;
import com.smartSure.authService.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository repo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private ModelMapper modelMapper;
    @Mock private OtpService otpService;

    @InjectMocks
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(1L);
        mockUser.setEmail("test@example.com");
        mockUser.setPassword("hashedPassword");
        mockUser.setRole(Role.CUSTOMER);
    }

    // ─── Register Tests ─────────────────────────────────────────────────────

    @Test
    void register_success() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("newuser@example.com");
        request.setPassword("password123");

        when(repo.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(modelMapper.map(any(), eq(User.class))).thenReturn(mockUser);
        when(passwordEncoder.encode(any())).thenReturn("hashedPassword");
        when(repo.save(any())).thenReturn(mockUser);

        String result = authService.register(request);

        assertEquals("User registered successfully", result);
        verify(repo).save(any());
    }

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(request));
        assertEquals("Email already registered", ex.getMessage());
        verify(repo, never()).save(any());
    }

    @Test
    void register_alwaysAssignsCustomerRole() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("hacker@example.com");
        request.setPassword("password");
        request.setRole("ADMIN"); // Attacker tries to self-register as ADMIN

        User userCapture = new User();
        when(repo.findByEmail(any())).thenReturn(Optional.empty());
        when(modelMapper.map(any(), eq(User.class))).thenReturn(userCapture);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(repo.save(any())).thenReturn(userCapture);

        authService.register(request);

        // SECURITY: Role must always be CUSTOMER regardless of request payload
        assertEquals(Role.CUSTOMER, userCapture.getRole());
    }

    // ─── Login Tests ────────────────────────────────────────────────────────

    @Test
    void login_success_sendOtp() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(otpService.generateAndSendOtp(any(), any(), any())).thenReturn("preAuthToken123");
        when(otpService.maskEmail(any())).thenReturn("te**@example.com");

        PreAuthResponseDto result = authService.login(request);

        assertNotNull(result);
        assertEquals("preAuthToken123", result.getPreAuthToken());
    }

    @Test
    void login_throwsWhenEmailNotFound() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("unknown@example.com");
        request.setPassword("password");

        when(repo.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(request));
        assertEquals("Invalid email or password", ex.getMessage());
    }

    @Test
    void login_throwsWhenPasswordWrong() {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("test@example.com");
        request.setPassword("wrongPassword");

        when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(request));
        // Generic message — prevents user enumeration
        assertEquals("Invalid email or password", ex.getMessage());
    }

    // ─── Verify OTP Tests ───────────────────────────────────────────────────

    @Test
    void verifyOtp_success_returnsJwtToken() {
        VerifyOtpRequestDto request = new VerifyOtpRequestDto();
        request.setPreAuthToken("preAuthToken123");
        request.setOtp("123456");

        OtpRecord mockRecord = new OtpRecord();
        mockRecord.setUserId(1L);
        mockRecord.setEmail("test@example.com");
        mockRecord.setRole("CUSTOMER");

        when(otpService.verifyOtp("preAuthToken123", "123456")).thenReturn(mockRecord);
        when(jwtUtil.generateToken(1L, "CUSTOMER")).thenReturn("jwt.token.here");

        AuthResponseDto result = authService.verifyOtp(request);

        assertNotNull(result);
        assertEquals("jwt.token.here", result.getToken());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void verifyOtp_throwsWhenOtpInvalid() {
        VerifyOtpRequestDto request = new VerifyOtpRequestDto();
        request.setPreAuthToken("invalidToken");
        request.setOtp("000000");

        when(otpService.verifyOtp(any(), any()))
                .thenThrow(new RuntimeException("Invalid or expired OTP"));

        assertThrows(RuntimeException.class, () -> authService.verifyOtp(request));
    }

    // ─── Create Admin Tests ─────────────────────────────────────────────────

    @Test
    void createAdmin_success() {
        AdminCreateRequestDto request = new AdminCreateRequestDto();
        request.setEmail("admin@example.com");
        request.setPassword("adminPass");

        User adminUser = new User();
        when(repo.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(modelMapper.map(any(), eq(User.class))).thenReturn(adminUser);
        when(passwordEncoder.encode(any())).thenReturn("hashedAdminPass");
        when(repo.save(any())).thenReturn(adminUser);

        String result = authService.createAdmin(request);

        assertEquals("Admin created successfully", result);
        assertEquals(Role.ADMIN, adminUser.getRole());
    }

    @Test
    void createAdmin_throwsWhenEmailExists() {
        AdminCreateRequestDto request = new AdminCreateRequestDto();
        request.setEmail("test@example.com");
        request.setPassword("adminPass");

        when(repo.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        assertThrows(RuntimeException.class, () -> authService.createAdmin(request));
        verify(repo, never()).save(any());
    }
}

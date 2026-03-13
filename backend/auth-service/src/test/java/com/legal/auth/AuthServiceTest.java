package com.legal.auth.service;

import com.legal.auth.dto.LoginRequest;
import com.legal.auth.dto.LoginResponse;
import com.legal.auth.dto.RegisterRequest;
import com.legal.auth.exception.AuthException;
import com.legal.auth.model.DeviceRegistration;
import com.legal.auth.model.User;
import com.legal.auth.repository.UserRepository;
import com.legal.auth.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private DeviceRegistrationService deviceService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordUtil passwordUtil;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$12$hashedPassword")
                .role(User.Role.USER)
                .authProvider(User.AuthProvider.LOCAL)
                .isActive(true)
                .isEmailVerified(true)
                .failedLoginAttempts(0)
                .build();
    }

    // ─── Register ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Registro exitoso de nuevo usuario")
    void register_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test User")
                .username("testuser")
                .email("test@example.com")
                .password("Password1!")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(), anyString())).thenReturn("refresh-token");
        when(jwtService.getJwtExpiration()).thenReturn(86400000L);
        when(deviceService.registerOrUpdateDevice(any(), any(), any(), any(), any(), any()))
                .thenReturn(new DeviceRegistration());

        LoginResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Registro falla si email ya existe")
    void register_EmailAlreadyExists_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .username("testuser")
                .build();

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("email ya está registrado");
    }

    // ─── Login ───────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Login exitoso con credenciales correctas")
    void login_Success() {
        LoginRequest request = new LoginRequest("test@example.com", "Password1!",
                "device-1", "Chrome", "WEB", null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("Password1!", testUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(), anyString())).thenReturn("refresh-token");
        when(jwtService.getJwtExpiration()).thenReturn(86400000L);
        when(deviceService.registerOrUpdateDevice(any(), any(), any(), any(), any(), any()))
                .thenReturn(new DeviceRegistration());

        LoginResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        verify(userRepository).resetFailedLoginAttempts(1L);
        verify(userRepository).updateLastLogin(eq(1L), any());
    }

    @Test
    @DisplayName("Login falla con usuario inexistente")
    void login_UserNotFound_ThrowsException() {
        LoginRequest request = new LoginRequest("noexiste@example.com", "Password1!",
                null, null, null, null);

        when(userRepository.findByEmail("noexiste@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Credenciales inválidas");
    }

    @Test
    @DisplayName("Login falla con cuenta desactivada")
    void login_InactiveUser_ThrowsException() {
        testUser.setIsActive(false);
        LoginRequest request = new LoginRequest("test@example.com", "Password1!",
                null, null, null, null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("desactivada");
    }

    @Test
    @DisplayName("Login falla con contraseña incorrecta e incrementa intentos fallidos")
    void login_WrongPassword_IncrementsFailedAttempts() {
        LoginRequest request = new LoginRequest("test@example.com", "WrongPassword1!",
                null, null, null, null);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("WrongPassword1!", testUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining("Credenciales inválidas");

        verify(userRepository).incrementFailedLoginAttempts(1L);
    }
}

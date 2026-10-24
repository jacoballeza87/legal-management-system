package com.legal.auth.service;

import com.legal.auth.dto.*;
import com.legal.auth.exception.AuthException;
import com.legal.auth.model.DeviceRegistration;
import com.legal.auth.model.User;
import com.legal.auth.repository.UserRepository;
import com.legal.auth.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final DeviceRegistrationService deviceService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordUtil passwordUtil;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    // ─── Registro ───────────────────────────────────────────────────────────────

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // Validar duplicados
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("El email ya está registrado");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthException("El username ya está en uso");
        }

        // Crear usuario
        User user = User.builder()
                .name(request.getName())
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : User.Role.USER)
                .authProvider(User.AuthProvider.LOCAL)
                .isActive(true)
                .isEmailVerified(false)
                .emailVerificationToken(UUID.randomUUID().toString())
                .build();

        user = userRepository.save(user);
        log.info("Nuevo usuario registrado: {} ({})", user.getUsername(), user.getEmail());

        return generateAuthResponse(user, null, null, null);
    }

    // ─── Login ───────────────────────────────────────────────────────────────────

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException("Credenciales inválidas"));

        // Verificar estado del usuario
        if (!user.getIsActive()) {
            throw new AuthException("Cuenta desactivada. Contacte al administrador");
        }

        // Verificar bloqueo por intentos fallidos
        if (user.isLocked()) {
            throw new AuthException("Cuenta temporalmente bloqueada. Intente nuevamente en " +
                    LOCK_DURATION_MINUTES + " minutos");
        }

        // Verificar que tenga contraseña (no sea OAuth only)
        if (user.getPasswordHash() == null) {
            throw new AuthException("Esta cuenta usa autenticación OAuth. Use Google o GitHub para ingresar");
        }

        // Verificar contraseña
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new AuthException("Credenciales inválidas");
        }

        // Login exitoso - reset intentos fallidos
        userRepository.resetFailedLoginAttempts(user.getId());
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());

        log.info("Login exitoso para usuario: {}", user.getEmail());
        return generateAuthResponse(user, request.getDeviceId(),
                request.getDeviceName(), request.getDeviceType());
    }

    // ─── Refresh Token ───────────────────────────────────────────────────────────

    @Transactional
    public TokenRefreshResponse refreshToken(TokenRefreshRequest request) {
        // Buscar device por refresh token
        DeviceRegistration device = deviceService.findByRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthException("Refresh token inválido o expirado"));

        // Verificar expiración
        if (device.isRefreshTokenExpired()) {
            deviceService.revokeDevice(device.getUser().getId(), device.getDeviceId());
            throw new AuthException("Refresh token expirado. Por favor inicie sesión nuevamente");
        }

        // Verificar JWT del refresh token
        User user = device.getUser();
        if (!jwtService.isTokenValid(request.getRefreshToken(), user.getEmail())) {
            throw new AuthException("Refresh token inválido");
        }

        // Generar nuevos tokens (rotación de refresh token)
        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user, device.getDeviceId());

        // Actualizar refresh token en BD
        deviceService.registerOrUpdateDevice(user, device.getDeviceId(),
                device.getDeviceName(), device.getDeviceType(),
                device.getIpAddress(), newRefreshToken);

        log.debug("Tokens renovados para usuario: {}", user.getEmail());

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .build();
    }

    // ─── Logout ──────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String accessToken, String deviceId, Long userId) {
        // Añadir access token a blacklist
        jwtService.blacklistToken(accessToken);

        // Revocar dispositivo específico
        if (deviceId != null && userId != null) {
            deviceService.revokeDevice(userId, deviceId);
        }

        log.info("Logout exitoso para userId: {}", userId);
    }

    @Transactional
    public void logoutAllDevices(String accessToken, Long userId) {
        jwtService.blacklistToken(accessToken);
        deviceService.revokeAllDevices(userId);
        log.info("Logout de todos los dispositivos para userId: {}", userId);
    }

    // ─── Cambio de contraseña ────────────────────────────────────────────────────

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AuthException("La contraseña actual es incorrecta");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AuthException("La nueva contraseña no puede ser igual a la actual");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Revocar todos los dispositivos para forzar nuevo login
        deviceService.revokeAllDevices(userId);
        log.info("Contraseña cambiada para usuario: {}", user.getEmail());
    }

    // ─── Reset de contraseña ─────────────────────────────────────────────────────

    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetExpires(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            // TODO: enviar email con token (via notification-service)
            log.info("Reset de contraseña solicitado para: {}", user.getEmail());
        });
        // Siempre responder OK para no revelar si el email existe
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new AuthException("Token de reset inválido o expirado"));

        if (user.getPasswordResetExpires().isBefore(LocalDateTime.now())) {
            throw new AuthException("El token de reset ha expirado");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpires(null);
        userRepository.save(user);

        deviceService.revokeAllDevices(user.getId());
        log.info("Contraseña reseteada para: {}", user.getEmail());
    }

    // ─── Helpers privados ────────────────────────────────────────────────────────

    private LoginResponse generateAuthResponse(User user, String deviceId,
                                                String deviceName, String deviceType) {
        String resolvedDeviceId = deviceId != null ? deviceId : UUID.randomUUID().toString();

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, resolvedDeviceId);

        // Registrar/actualizar dispositivo
        deviceService.registerOrUpdateDevice(user, resolvedDeviceId,
                deviceName, deviceType, null, refreshToken);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getJwtExpiration() / 1000)
                .deviceId(resolvedDeviceId)
                .user(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .avatarUrl(user.getAvatarUrl())
                        .build())
                .build();
    }

    private void handleFailedLogin(User user) {
        userRepository.incrementFailedLoginAttempts(user.getId());
        int attempts = user.getFailedLoginAttempts() + 1;
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
            userRepository.lockUser(user.getId(), lockUntil);
            log.warn("Cuenta bloqueada por {} intentos fallidos: {}", attempts, user.getEmail());
        }
    }
}

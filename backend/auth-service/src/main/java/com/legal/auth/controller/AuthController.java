package com.legal.auth.controller;

import com.legal.auth.dto.*;
import com.legal.auth.service.AuthService;
import com.legal.auth.service.DeviceRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Endpoints de autenticación y autorización")
public class AuthController {

    private final AuthService authService;
    private final DeviceRegistrationService deviceService;

    @PostMapping("/register")
    @Operation(summary = "Registro de nuevo usuario")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login con email y contraseña")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                                HttpServletRequest httpRequest) {
        // Añadir IP del request si no viene en el body
        if (request.getIpAddress() == null) {
            request.setIpAddress(getClientIp(httpRequest));
        }
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar access token con refresh token")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        TokenRefreshResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión en el dispositivo actual")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        Long userId = (Long) request.getAttribute("userId");
        String deviceId = (String) request.getAttribute("deviceId");

        authService.logout(token, deviceId, userId);
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada exitosamente"));
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Cerrar sesión en todos los dispositivos")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> logoutAll(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        Long userId = (Long) request.getAttribute("userId");

        authService.logoutAllDevices(token, userId);
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada en todos los dispositivos"));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Cambiar contraseña")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        Long userId = (Long) httpRequest.getAttribute("userId");
        authService.changePassword(userId, request);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada exitosamente"));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar reset de contraseña")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(Map.of("message",
                "Si el email existe, recibirás instrucciones para restablecer tu contraseña"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Restablecer contraseña con token")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Contraseña restablecida exitosamente"));
    }

    @GetMapping("/devices")
    @Operation(summary = "Obtener dispositivos registrados del usuario")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getDevices(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(deviceService.getUserDevices(userId));
    }

    @DeleteMapping("/devices/{deviceId}")
    @Operation(summary = "Revocar un dispositivo específico")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> revokeDevice(
            @PathVariable String deviceId,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        deviceService.revokeDevice(userId, deviceId);
        return ResponseEntity.ok(Map.of("message", "Dispositivo revocado exitosamente"));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validar token (usado por API Gateway)")
    @SecurityRequirement(name = "bearerAuth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> validateToken(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String deviceId = (String) request.getAttribute("deviceId");
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "userId", userId,
                "deviceId", deviceId != null ? deviceId : ""
        ));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private String extractTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

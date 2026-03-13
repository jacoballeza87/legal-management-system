package com.legal.auth.service;

import com.legal.auth.exception.DeviceLimitExceededException;
import com.legal.auth.model.DeviceRegistration;
import com.legal.auth.model.User;
import com.legal.auth.repository.DeviceRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceRegistrationService {

    private final DeviceRegistrationRepository deviceRepository;
    private final JwtService jwtService;

    @Value("${device.max-per-user:3}")
    private int maxDevicesPerUser;

    @Transactional
    public DeviceRegistration registerOrUpdateDevice(User user, String deviceId,
                                                      String deviceName, String deviceType,
                                                      String ipAddress, String refreshToken) {
        // Si ya existe el device, actualizar
        Optional<DeviceRegistration> existing = deviceRepository
                .findByUserIdAndDeviceId(user.getId(), deviceId);

        if (existing.isPresent()) {
            DeviceRegistration device = existing.get();
            device.setRefreshToken(refreshToken);
            device.setRefreshTokenExpires(LocalDateTime.now()
                    .plusSeconds(jwtService.getRefreshExpiration() / 1000));
            device.setLastUsed(LocalDateTime.now());
            device.setIpAddress(ipAddress);
            return deviceRepository.save(device);
        }

        // Verificar límite de dispositivos
        long currentDevices = deviceRepository.countByUserId(user.getId());
        if (currentDevices >= maxDevicesPerUser) {
            // Eliminar el dispositivo más antiguo (LRU policy)
            List<DeviceRegistration> devices = deviceRepository
                    .findByUserIdOrderByLastUsedAsc(user.getId());
            if (!devices.isEmpty()) {
                log.info("Límite de dispositivos alcanzado para usuario {}. Eliminando dispositivo más antiguo: {}",
                        user.getId(), devices.get(0).getDeviceId());
                deviceRepository.delete(devices.get(0));
            }
        }

        // Crear nuevo registro de dispositivo
        DeviceRegistration device = DeviceRegistration.builder()
                .user(user)
                .deviceId(deviceId != null ? deviceId : UUID.randomUUID().toString())
                .deviceName(deviceName)
                .deviceType(deviceType)
                .ipAddress(ipAddress)
                .refreshToken(refreshToken)
                .refreshTokenExpires(LocalDateTime.now()
                        .plusSeconds(jwtService.getRefreshExpiration() / 1000))
                .lastUsed(LocalDateTime.now())
                .isTrusted(false)
                .build();

        return deviceRepository.save(device);
    }

    @Transactional(readOnly = true)
    public Optional<DeviceRegistration> findByRefreshToken(String refreshToken) {
        return deviceRepository.findByRefreshToken(refreshToken);
    }

    @Transactional
    public void revokeDevice(Long userId, String deviceId) {
        deviceRepository.deleteByUserIdAndDeviceId(userId, deviceId);
        log.info("Dispositivo {} revocado para usuario {}", deviceId, userId);
    }

    @Transactional
    public void revokeAllDevices(Long userId) {
        deviceRepository.deleteAllByUserId(userId);
        log.info("Todos los dispositivos revocados para usuario {}", userId);
    }

    @Transactional(readOnly = true)
    public List<DeviceRegistration> getUserDevices(Long userId) {
        return deviceRepository.findByUserId(userId);
    }

    @Transactional
    public void updateLastUsed(Long deviceId) {
        deviceRepository.updateLastUsed(deviceId, LocalDateTime.now());
    }

    // Limpieza automática de tokens expirados (cada día a las 2:00 AM)
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanExpiredTokens() {
        log.info("Limpiando refresh tokens expirados...");
        deviceRepository.deleteExpiredTokens(LocalDateTime.now());
    }
}

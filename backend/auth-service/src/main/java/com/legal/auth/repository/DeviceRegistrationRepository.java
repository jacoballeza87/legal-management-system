package com.legal.auth.repository;

import com.legal.auth.model.DeviceRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceRegistrationRepository extends JpaRepository<DeviceRegistration, Long> {

    List<DeviceRegistration> findByUserId(Long userId);

    Optional<DeviceRegistration> findByUserIdAndDeviceId(Long userId, String deviceId);

    Optional<DeviceRegistration> findByRefreshToken(String refreshToken);

    long countByUserId(Long userId);

    boolean existsByUserIdAndDeviceId(Long userId, String deviceId);

    @Modifying
    @Query("DELETE FROM DeviceRegistration d WHERE d.user.id = :userId AND d.deviceId = :deviceId")
    void deleteByUserIdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);

    @Modifying
    @Query("DELETE FROM DeviceRegistration d WHERE d.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM DeviceRegistration d WHERE d.refreshTokenExpires < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE DeviceRegistration d SET d.lastUsed = :lastUsed WHERE d.id = :id")
    void updateLastUsed(@Param("id") Long id, @Param("lastUsed") LocalDateTime lastUsed);

    @Query("SELECT d FROM DeviceRegistration d WHERE d.user.id = :userId ORDER BY d.lastUsed ASC")
    List<DeviceRegistration> findByUserIdOrderByLastUsedAsc(@Param("userId") Long userId);
}

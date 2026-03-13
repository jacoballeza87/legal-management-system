package com.legal.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_identifier", length = 255)
    private String deviceId;

    @Column(name = "device_name", length = 100)
    private String deviceName;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "os_info", length = 100)
    private String osInfo;

    @Column(name = "browser_info", length = 100)
    private String browserInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Column(name = "refresh_token_expires")
    private LocalDateTime refreshTokenExpires;

    @Column(name = "is_trusted")
    @Builder.Default
    private Boolean isTrusted = false;

    @Column(name = "last_used")
    private LocalDateTime lastUsed;

    @CreationTimestamp
    @Column(name = "registered_at", updatable = false)
    private LocalDateTime registeredAt;

    public boolean isRefreshTokenExpired() {
        return refreshTokenExpires != null && refreshTokenExpires.isBefore(LocalDateTime.now());
    }
}

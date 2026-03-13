package com.legal.auth.service;

import com.legal.auth.dto.LoginResponse;
import com.legal.auth.model.User;
import com.legal.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2Service {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final DeviceRegistrationService deviceService;

    @Transactional
    public LoginResponse processOAuthLogin(OAuth2User oAuth2User, String provider,
                                            String deviceId, String deviceName, String deviceType) {
        UserOAuthInfo info = extractUserInfo(oAuth2User, provider);
        User user = findOrCreateOAuthUser(info, provider);

        String resolvedDeviceId = deviceId != null ? deviceId : UUID.randomUUID().toString();
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, resolvedDeviceId);

        deviceService.registerOrUpdateDevice(user, resolvedDeviceId,
                deviceName, deviceType, null, refreshToken);

        log.info("OAuth login exitoso via {} para: {}", provider, user.getEmail());

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

    private User findOrCreateOAuthUser(UserOAuthInfo info, String provider) {
        User.AuthProvider authProvider = User.AuthProvider.valueOf(provider.toUpperCase());

        // Buscar por provider ID
        Optional<User> byProviderId = userRepository
                .findByProviderIdAndAuthProvider(info.providerId(), authProvider);
        if (byProviderId.isPresent()) {
            User user = byProviderId.get();
            user.setAvatarUrl(info.avatarUrl());
            return userRepository.save(user);
        }

        // Buscar por email (usuario local que quiere vincular OAuth)
        Optional<User> byEmail = userRepository.findByEmail(info.email());
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            user.setAuthProvider(authProvider);
            user.setProviderId(info.providerId());
            user.setAvatarUrl(info.avatarUrl());
            user.setIsEmailVerified(true);
            return userRepository.save(user);
        }

        // Crear nuevo usuario OAuth
        String username = generateUniqueUsername(info.name());
        User newUser = User.builder()
                .name(info.name())
                .username(username)
                .email(info.email())
                .authProvider(authProvider)
                .providerId(info.providerId())
                .avatarUrl(info.avatarUrl())
                .role(User.Role.USER)
                .isActive(true)
                .isEmailVerified(true)
                .build();

        log.info("Nuevo usuario OAuth creado via {}: {}", provider, info.email());
        return userRepository.save(newUser);
    }

    private UserOAuthInfo extractUserInfo(OAuth2User oAuth2User, String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> new UserOAuthInfo(
                    oAuth2User.getAttribute("sub"),
                    oAuth2User.getAttribute("name"),
                    oAuth2User.getAttribute("email"),
                    oAuth2User.getAttribute("picture")
            );
            case "github" -> new UserOAuthInfo(
                    String.valueOf(oAuth2User.getAttribute("id")),
                    oAuth2User.getAttribute("name"),
                    oAuth2User.getAttribute("email"),
                    oAuth2User.getAttribute("avatar_url")
            );
            default -> throw new IllegalArgumentException("Proveedor OAuth no soportado: " + provider);
        };
    }

    private String generateUniqueUsername(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9]", "")
                .substring(0, Math.min(name.length(), 20));
        String username = base;
        int counter = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + counter++;
        }
        return username;
    }

    private record UserOAuthInfo(String providerId, String name, String email, String avatarUrl) {}
}

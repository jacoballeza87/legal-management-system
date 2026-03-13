package com.legal.auth.service;

import com.legal.auth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService Tests")
class JwtServiceTest {

    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private JwtService jwtService;
    private User testUser;

    // Clave base64 de prueba (256 bits)
    private static final String TEST_SECRET =
            "dGhpcy1pcy1hLXZlcnktc2VjdXJlLXNlY3JldC1rZXktZm9yLXRlc3RpbmctcHVycG9zZXMtb25seQ==";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(redisTemplate);
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 86400000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L);
        ReflectionTestUtils.setField(jwtService, "issuer", "legal-management-system");

        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .username("testuser")
                .email("test@example.com")
                .role(User.Role.USER)
                .build();
    }

    @Test
    @DisplayName("Generar access token correctamente")
    void generateAccessToken_ShouldReturnValidToken() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(1L);
        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
        assertThat(jwtService.extractTokenType(token)).isEqualTo("ACCESS");
    }

    @Test
    @DisplayName("Generar refresh token correctamente")
    void generateRefreshToken_ShouldReturnValidToken() {
        String token = jwtService.generateRefreshToken(testUser, "device-123");

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtService.extractEmail(token)).isEqualTo("test@example.com");
        assertThat(jwtService.extractDeviceId(token)).isEqualTo("device-123");
        assertThat(jwtService.extractTokenType(token)).isEqualTo("REFRESH");
    }

    @Test
    @DisplayName("Token válido pasa validación")
    void isTokenValid_ValidToken_ReturnsTrue() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String token = jwtService.generateAccessToken(testUser);

        boolean valid = jwtService.isTokenValid(token, "test@example.com");

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Token con email incorrecto no pasa validación")
    void isTokenValid_WrongEmail_ReturnsFalse() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        String token = jwtService.generateAccessToken(testUser);

        boolean valid = jwtService.isTokenValid(token, "otro@example.com");

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Token en blacklist no pasa validación")
    void isTokenValid_BlacklistedToken_ReturnsFalse() {
        String token = jwtService.generateAccessToken(testUser);
        when(redisTemplate.hasKey(anyString())).thenReturn(true);

        boolean valid = jwtService.isTokenValid(token, "test@example.com");

        assertThat(valid).isFalse();
    }
}

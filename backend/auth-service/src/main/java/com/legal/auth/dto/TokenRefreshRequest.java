package com.legal.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TokenRefreshRequest {
    @NotBlank(message = "El refresh token es obligatorio")
    private String refreshToken;

    private String deviceId;
}

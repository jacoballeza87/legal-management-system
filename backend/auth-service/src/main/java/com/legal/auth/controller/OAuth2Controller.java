package com.legal.auth.controller;
import org.springframework.http.HttpStatus;
import com.legal.auth.dto.LoginResponse;
import com.legal.auth.service.OAuth2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "OAuth2", description = "Endpoints de autenticación social (Google, GitHub)")
public class OAuth2Controller {

    private final OAuth2Service oAuth2Service;

    @GetMapping("/callback/{provider}")
    @Operation(summary = "Callback OAuth2 tras autenticación con el proveedor")
    public ResponseEntity<LoginResponse> oauthCallback(
            @PathVariable String provider,
            @AuthenticationPrincipal OAuth2User oAuth2User,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String deviceName,
            @RequestParam(required = false) String deviceType,
            HttpServletRequest request) {

        if (oAuth2User == null) {
            log.warn("OAuth2 callback sin usuario autenticado para provider: {}", provider);
return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        LoginResponse response = oAuth2Service.processOAuthLogin(
                oAuth2User, provider, deviceId, deviceName, deviceType);

        return ResponseEntity.ok(response);
    }
}

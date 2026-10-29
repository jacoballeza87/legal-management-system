package com.legal.auth.service;

import com.legal.auth.dto.JwtResponse;
import com.legal.auth.dto.LoginRequest;
import com.legal.auth.model.User;
import com.legal.auth.repository.UserRepository;
import com.legal.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtTokenProvider tokenProvider;

    public JwtResponse authenticateUser(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con el correo: " + loginRequest.getEmail()));

        // RN-S-007: Validar que el usuario no tenga rol PENDING
        if ("PENDING".equalsIgnoreCase(user.getRole().name())) {
            throw new IllegalStateException("Acceso denegado: Tu cuenta se encuentra en estado PENDING pendiente de aprobación.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // FIX: Added .name() to convert Enum to String
        String jwt = tokenProvider.generateToken(authentication, user.getRole().name(), user.getCategory());

        return JwtResponse.builder()
                .token(jwt)
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole()) // Assumes JwtResponse accepts the Enum, if it wants a String use .name()
                .category(user.getCategory())
                .build();
    }
}

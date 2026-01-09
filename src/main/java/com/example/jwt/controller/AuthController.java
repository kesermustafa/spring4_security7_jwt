package com.example.jwt.controller;

import com.example.jwt.domain.dtos.request.LoginRequest;
import com.example.jwt.domain.dtos.request.RefreshTokenRequest;
import com.example.jwt.domain.dtos.request.RegisterRequest;
import com.example.jwt.domain.dtos.response.ApiResponse;
import com.example.jwt.domain.dtos.response.AuthResponse;
import com.example.jwt.domain.dtos.response.TokenRefreshResponse;
import com.example.jwt.exception.messages.SuccessMessages;
import com.example.jwt.services.AuthService;
import com.example.jwt.services.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refresh(@RequestBody RefreshTokenRequest request) {

        AuthResponse auth = refreshTokenService.rotateRefreshToken(request.refreshToken());

        TokenRefreshResponse response = new TokenRefreshResponse(
                auth.accessToken(),
                auth.refreshToken()
        );

        return ResponseEntity.ok(response);
    }


    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        refreshTokenService.logoutAllDevicesByEmail(email);

        return ResponseEntity.ok(Map.of(
                "message", SuccessMessages.LOGOUT_SUCCESSFUL
        ));
    }

}

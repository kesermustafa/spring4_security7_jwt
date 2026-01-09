package com.example.jwt.domain.dtos.response;

public record TokenRefreshResponse(
        String accessToken,
        String refreshToken
) {}

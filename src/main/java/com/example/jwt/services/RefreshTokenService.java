package com.example.jwt.services;

import com.example.jwt.domain.model.RefreshToken;
import com.example.jwt.domain.model.User;
import com.example.jwt.domain.dtos.response.AuthResponse;
import com.example.jwt.exception.BadRequestException;
import com.example.jwt.repository.RefreshTokenRepository;
import com.example.jwt.repository.UserRepository;
import com.example.jwt.security.JwtService;
import com.example.jwt.utils.TokenHashUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final TokenHashUtil tokenHashUtil;


    @Transactional
    public String createRefreshToken(User user) {

        RefreshToken token = refreshTokenRepository.findByUserId(user.getId())
                .orElseGet(() -> RefreshToken.builder()
                        .user(user)
                        .build()
                );

        String refreshJwt = jwtService.generateRefreshToken(user.getId());
        String hash = tokenHashUtil.sha256(refreshJwt);

        token.setTokenHash(hash);
        token.setExpiryDate(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()));

        refreshTokenRepository.save(token);
        refreshTokenRepository.flush();

        return refreshJwt;
    }



    @Transactional
    public AuthResponse rotateRefreshToken(String rawRefreshToken) {

        String hashedToken = tokenHashUtil.sha256(rawRefreshToken);

        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> {
                    handleRefreshTokenReuse(rawRefreshToken);
                    return new SecurityException("Refresh token reuse detected – all sessions revoked");
                });

        if (oldToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(oldToken);
            refreshTokenRepository.flush();
            throw new BadRequestException("Refresh token expired");
        }

        User user = oldToken.getUser();

        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.flush();

        String newRefreshRaw = jwtService.generateRefreshToken(user.getId());
        String newRefreshHash = tokenHashUtil.sha256(newRefreshRaw);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newRefreshHash)
                .expiryDate(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()))
                .build();

        refreshTokenRepository.saveAndFlush(newRefreshToken);

        String newAccessToken = jwtService.generateAccessToken(user.getEmail());

        return new AuthResponse(newAccessToken, newRefreshRaw);
    }


    @Transactional
    public void logoutAllDevicesByEmail(String email) {
        refreshTokenRepository.deleteAllByUserEmail(email);
    }


    private void handleRefreshTokenReuse(String rawToken) {
        Optional<User> userOpt = userRepository.findByEmail(jwtService.extractEmailIgnoringExpiration(rawToken));
        userOpt.ifPresent(user -> refreshTokenRepository.deleteByUserId(user.getId()));
    }


    public String generateAccessToken(User user) {
        return jwtService.generateAccessToken(user.getEmail());
    }
}

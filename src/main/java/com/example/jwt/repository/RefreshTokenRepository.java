package com.example.jwt.repository;

import com.example.jwt.domain.model.RefreshToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Modifying
    @Transactional
    void deleteByUserId(UUID userId);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    Optional<RefreshToken> findByUserId(UUID userId);


    @Transactional
    void deleteAllByUserId(UUID userId);

    void deleteAllByUserEmail(String email);
}

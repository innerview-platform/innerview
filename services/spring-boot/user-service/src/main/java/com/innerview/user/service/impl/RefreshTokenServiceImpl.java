package com.innerview.user.service.impl;

import com.innerview.user.entity.RefreshToken;
import com.innerview.user.entity.User;
import com.innerview.user.repository.RefreshTokenRepository;
import com.innerview.user.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.innerview.user.core.util.JwtUtil;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    private final JwtUtil jwtUtil;

    @Value("${jwt.refresh-token.expiration}")
    private Long refreshTokenExpiration; // 7 days in milliseconds



    @Transactional
    public RefreshToken createRefreshToken(User user) {
        // Delete any existing refresh token for this user
        refreshTokenRepository.deleteById(user.getId());

        // Generate new refresh token
        String tokenString = jwtUtil.generateRefreshToken(user.getId());

        LocalDateTime expiresAt = LocalDateTime.now()
                .plusSeconds(refreshTokenExpiration / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenString)
                .user(user)
                .expiresAt(expiresAt)
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public boolean isValidRefreshToken(RefreshToken refreshToken) {
        if (refreshToken == null) {
            return false;
        }

        if (refreshToken.getRevoked()) {
            log.warn("Refresh token is revoked: {}", refreshToken.getId());
            return false;
        }

        if (refreshToken.isExpired()) {
            log.warn("Refresh token is expired: {}", refreshToken.getId());
            return false;
        }

        return true;
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(refreshToken -> {
                    refreshToken.revoke();
                    refreshTokenRepository.save(refreshToken);
                    log.info("Revoked refresh token: {}", refreshToken.getId());
                });
    }

    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.deleteById(user.getId());
        log.info("Revoked all refresh tokens for user: {}", user.getId());
    }

    @Transactional
    public int cleanupExpiredAndRevokedTokens() {
        int deletedCount = refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
        log.info("Cleanup Job: Deleted {} expired/revoked tokens", deletedCount);
        return deletedCount;
    }
}

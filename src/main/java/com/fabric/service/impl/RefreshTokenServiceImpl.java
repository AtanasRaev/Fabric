package com.fabric.service.impl;

import com.fabric.database.entity.RefreshToken;
import com.fabric.database.repository.RefreshTokenRepository;
import com.fabric.service.RefreshTokenService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void saveNewToken(String tokenId, String userEmail, Instant expiryDate) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenId(tokenId);
        refreshToken.setUserEmail(userEmail);
        refreshToken.setExpiryDate(expiryDate);
        refreshToken.setRevoked(false);
        this.refreshTokenRepository.save(refreshToken);
    }

    @Override
    public boolean isValid(String tokenId) {
        return this.refreshTokenRepository.findByTokenId(tokenId)
                .filter(token -> !token.isRevoked() && token.getExpiryDate().isAfter(Instant.now()))
                .isPresent();
    }

    @Override
    public void revokeToken(String tokenId) {
        this.refreshTokenRepository.findByTokenId(tokenId).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Scheduled(cron = "0 0 */5 * * ?")
    @Transactional
    protected void cleanupExpiredTokens() {
        this.refreshTokenRepository.deleteByExpiryDateBefore(Instant.now());
    }
}

package com.innerview.spring.service;

import org.springframework.transaction.annotation.Transactional;
import com.innerview.spring.entity.RefreshToken;
import com.innerview.spring.entity.User;

import java.util.Optional;

public interface RefreshTokenService {
	public RefreshToken createRefreshToken(User user);

	public String createAccessToken(User user);

	public Optional<RefreshToken> findByToken(String token);

	public boolean isValidRefreshToken(RefreshToken refreshToken);

	@Transactional
	public void revokeToken(String token);

	@Transactional
	public void revokeAllUserTokens(User user);

	@Transactional
	public int cleanupExpiredAndRevokedTokens();
}

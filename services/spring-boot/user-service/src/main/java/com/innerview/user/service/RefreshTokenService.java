package com.innerview.user.service;

import org.springframework.transaction.annotation.Transactional;
import com.innerview.user.entity.RefreshToken;
import com.innerview.user.entity.User;

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

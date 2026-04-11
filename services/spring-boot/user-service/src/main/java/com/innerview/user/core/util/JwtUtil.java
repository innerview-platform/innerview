package com.innerview.user.core.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil {
	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.access-token.expiration}")
	private Long accessTokenExpiration; // 15 minutes in milliseconds

	@Value("${jwt.refresh-token.expiration}")
	private Long refreshTokenExpiration; // 7 days in milliseconds

	private Key getSigningKey() {
		byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public String generateAccessToken(UUID userId) {
		Map<String, Object> claims = new HashMap<>();
		return createToken(claims, userId.toString(), accessTokenExpiration);
	}

	public String generateRefreshToken(UUID userId) {
		Map<String, Object> claims = new HashMap<>();
		return createToken(claims, userId.toString(), refreshTokenExpiration);
	}

	private String createToken(Map<String, Object> claims, String subject, Long expiration) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expiration);

		return Jwts.builder()
				.setClaims(claims)
				.setSubject(subject)
				.setIssuedAt(now)
				.setExpiration(expiryDate)
				.signWith(getSigningKey(), SignatureAlgorithm.HS256)
				.compact();
	}

	public UUID extractUserId(String token) {
		String userId = extractClaim(token, Claims::getSubject);
		return UUID.fromString(userId);
	}

	public Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	private Claims extractAllClaims(String token) {
		try {
			return Jwts.parserBuilder()
					.setSigningKey(getSigningKey())
					.build()
					.parseClaimsJws(token)
					.getBody();
		} catch (JwtException e) {
			throw new JwtException("Invalid or expired JWT token", e);
		}
	}

	public boolean isTokenExpired(String token) {
		try {
			return extractExpiration(token).before(new Date());
		} catch (JwtException e) {
			return true;
		}
	}

	public boolean validateToken(String token) {
		try {
			extractAllClaims(token);
			return !isTokenExpired(token);
		} catch (JwtException e) {
			return false;
		}
	}
}

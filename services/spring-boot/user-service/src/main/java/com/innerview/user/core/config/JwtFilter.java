package com.innerview.user.core.config;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.innerview.user.core.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

	private final JwtUtil jwtUtil;

	// DELETED: private final UserRepository userRepository;

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain) throws ServletException, IOException {

		try {
			String jwt = extractJwtFromHeader(request);

			if (jwt != null && jwtUtil.validateToken(jwt)) {
				//extract the ID directly from the token payload
				UUID currentUserId = jwtUtil.extractUserId(jwt);

				//Put the UUID directly into the SecurityContext, NO database query!
				UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(
								currentUserId, // The Principal is now just the UUID string
								null,
								Collections.emptyList()
						);

				authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);

				log.debug("Stateless authentication successful for user ID: {}", currentUserId);
			}
		} catch (JwtException e) {
			log.error("JWT validation failed: {}", e.getMessage());
		} catch (Exception e) {
			log.error("Authentication error: {}", e.getMessage());
		}

		filterChain.doFilter(request, response);
	}

	private String extractJwtFromHeader(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}
}
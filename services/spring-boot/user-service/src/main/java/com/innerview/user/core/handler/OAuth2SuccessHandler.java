package com.innerview.user.core.handler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import com.innerview.user.entity.User;
import com.innerview.user.repository.UserRepository;
import com.innerview.user.service.RefreshTokenService;
import com.innerview.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
	@Value("${jwt.access-token.expiration}")
	int accessTokenExpiry;

	@Value("${jwt.refresh-token.expiration}")
	int refreshTokenExpiry;

	private final UserService userService;
	private final UserRepository userRepository;
	private final RefreshTokenService tokenService;

	public OAuth2SuccessHandler(
			UserService userService, UserRepository userRepository, RefreshTokenService tokenService) {
		this.userService = userService;
		this.userRepository = userRepository;
		this.tokenService = tokenService;
	}

	@Override
	public void onAuthenticationSuccess(
			HttpServletRequest request, HttpServletResponse response, Authentication authentication)
			throws IOException {
		OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
		String email = oauthUser.getAttribute("email");
		String name = oauthUser.getAttribute("name");
		String providerId = oauthUser.getAttribute("sub");
		String refreshToken;
		String accessToken;
		Optional<User> user = userRepository.findByProviderId(providerId);
		// if he is logging back with his google account
		if (user.isPresent()) {
			refreshToken = tokenService.createRefreshToken(user.get()).getToken();
			accessToken = tokenService.createAccessToken(user.get());
		} else {
			user = userRepository.findByEmail(email);
			// in case he he is logging back but registerd with normal email and password not by google
			if (user.isPresent()) {
				refreshToken = tokenService.createRefreshToken(user.get()).getToken();
				accessToken = tokenService.createAccessToken(user.get());
				// we need to link the local account with google provider id
				User modifiedUser = user.get();
				modifiedUser.setProviderId(providerId);
				userRepository.save(modifiedUser);
			} else {
				// first time user visiting the website
				User newUser =
						User.builder()
								.authProvider("Google")
								.providerId(providerId)
								.name(name)
								.email(email)
								.passwordHash(null)
								.build();
				User savedUser = userRepository.save(newUser);
				refreshToken = tokenService.createRefreshToken(savedUser).getToken();
				accessToken = tokenService.createAccessToken(savedUser);
			}
		}
		Cookie accessTokenCookie = new Cookie("access_token", accessToken);
		accessTokenCookie.setHttpOnly(true);
		accessTokenCookie.setPath("/");
		accessTokenCookie.setMaxAge(accessTokenExpiry / 1000);

		Cookie refreshTokenCookie = new Cookie("refresh_token", refreshToken);
		refreshTokenCookie.setHttpOnly(true);
		refreshTokenCookie.setPath("/");
		refreshTokenCookie.setMaxAge(refreshTokenExpiry / 1000);
		response.addCookie(accessTokenCookie);
		response.addCookie(refreshTokenCookie);
		// we should change that to frontend url/homepage or similar
		response.sendRedirect("/api/auth/dashboard-test");
	}
}

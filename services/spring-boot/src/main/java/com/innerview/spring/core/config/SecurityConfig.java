package com.innerview.spring.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import com.innerview.spring.core.handler.OAuth2SuccessHandler;
import lombok.RequiredArgsConstructor;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	private final JwtFilter jwtAuthenticationFilter;
	private final OAuth2SuccessHandler successHandler;

	@Value("${frontend.url}")
	String frontendUrl;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable).sessionManagement(
						// Google Oauth requires the session to store state paramter to complete the flow
						// So, I modified it from stateless to IF_REQUIRED
						session -> session
								.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/login",
								"/api/auth/signup",
								"/api/auth/refresh",
								"/api/auth/google/login",
								"/api/auth/register",
								"/api/auth/forgot-password").
						permitAll().anyRequest().authenticated())
				.cors(cors -> cors
						.configurationSource(request -> {
							CorsConfiguration config = new CorsConfiguration();
							config.setAllowedOrigins(List.of(frontendUrl));
							config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
							config.setAllowedHeaders(List.of("*"));
							config.setAllowCredentials(true); // MUST be true for cookies to work
							return config;
						})).oauth2Login(oauth2 -> oauth2.successHandler(successHandler).failureHandler((request, response, exception) -> {
					// we should redirect to frontend path mainly the login page with message
					// saying that the authentication failed
					String encodedMessage = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
					response.sendRedirect(frontendUrl + "/login?error=" + encodedMessage);
				})).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}

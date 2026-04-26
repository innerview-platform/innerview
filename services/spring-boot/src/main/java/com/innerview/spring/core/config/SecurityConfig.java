package com.innerview.spring.core.config;

import com.innerview.spring.core.handler.OAuth2SuccessHandler;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtFilter jwtAuthenticationFilter;
  private final OAuth2SuccessHandler successHandler;
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

  @Value("${frontend.url}")
  private String frontendUrl;

  // Helper method to keep CORS config DRY across both filter chains
  private void applyCors(HttpSecurity http) throws Exception {
    http.cors(
        cors ->
            cors.configurationSource(
                request -> {
                  CorsConfiguration config = new CorsConfiguration();
                  config.setAllowedOrigins(List.of(frontendUrl));
                  config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                  config.setAllowedHeaders(List.of("*"));
                  config.setAllowCredentials(true);
                  return config;
                }));
  }

  @Bean
  @Order(1)
  public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/**") // This chain intercepts ONLY /api/ requests
        .csrf(AbstractHttpConfigurer::disable);

    applyCors(http);
    // made the session Stateless
    http.sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/auth/login",
                        "/api/auth/signup",
                        "/api/auth/refresh",
                        "/api/auth/google/login",
                        "/api/auth/register",
                        "/api/auth/forgot-password")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            exceptions -> exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {

    // No securityMatcher here, so it acts as a catchfor OAuth & Login routes
    http.csrf(AbstractHttpConfigurer::disable);

    applyCors(http);

    http
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .authorizeHttpRequests(
            auth -> auth.anyRequest().permitAll()) // Let the OAuth filters handle authorization
        .oauth2Login(
            oauth2 ->
                oauth2
                    .successHandler(successHandler)
                    .failureHandler(
                        (request, response, exception) -> {
                          String encodedMessage =
                              URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
                          response.sendRedirect(frontendUrl + "/login?error=" + encodedMessage);
                        }));
    return http.build();
  }
}

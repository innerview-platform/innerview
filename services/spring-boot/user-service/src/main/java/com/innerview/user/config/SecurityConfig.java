package com.innerview.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration for the application.
 *
 * <p>IMPORTANT: This current setup is strictly for testing the OAuth2 login flow. Upon a successful
 * login, the user is intentionally redirected to YouTube to watch somrthing cool inshallah.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/auth/**", "/error")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2Login(
            oauth2 ->
                oauth2.successHandler(
                    (request, response, authentication) -> {
                      response.sendRedirect("http://localhost:8080/login/oauth2/code/google");
                    }));

    return http.build();
  }
}


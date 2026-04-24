package com.innerview.spring.core.config;

import com.innerview.spring.core.handler.OAuth2SuccessHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestConfiguration
public class TestSecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    // Mock the specific dependencies your real SecurityConfig was looking for
    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private OAuth2SuccessHandler oAuth2SuccessHandler;
}
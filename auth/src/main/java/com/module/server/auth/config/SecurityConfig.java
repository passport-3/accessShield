package com.module.server.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);

        http
            .authorizeHttpRequests(authz -> authz
                    .requestMatchers("/api/auth/login").permitAll() // 인증을 요구하지 않는 URL
                    .requestMatchers("/api/user/login").permitAll()
                    .requestMatchers("/api/auth/token").permitAll() // 인증을 요구하지 않는 URL
                    .requestMatchers("/api/auth/verify").permitAll() // 인증을 요구하지 않는 URL
                    .requestMatchers("/api/auth/reIssue").permitAll() // 인증을 요구하지 않는 URL
                    .anyRequest().authenticated() // 나머지 요청은 인증 필요
            );

        return http.build();
    }
}
